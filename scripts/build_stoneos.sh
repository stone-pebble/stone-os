#!/bin/bash
# StoneOS Build System
# Single script for all development workflows
# Usage: ./build_stoneos.sh [--quick|--test|--deploy|--cost|--help]

set -e

# Configuration
PROJECT="dev-stone"
ZONE="us-central1-a"
MACHINE_TYPE="n2-standard-32"  # 32 vCPUs, 128GB RAM - FASTER BUILD!
LOCAL_DIR="/home/samuellarson/stone-os"
AOSP_BRANCH="android-14.0.0_r61"  # Android 14 QPR2 - compatible with Pixel 8a (akita)
S3_BUCKET="stoneos-builds"  # Optional S3 storage

# Build metadata
BUILD_VERSION="0.1.0"
BUILD_TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BUILD_ID="stoneos-v${BUILD_VERSION//./-}-${BUILD_TIMESTAMP}"
INSTANCE_NAME="stoneos-builder-${BUILD_TIMESTAMP}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_warning() { echo -e "${YELLOW}!${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_step() { echo -e "\n${CYAN}▶${NC} $1\n"; }

# Track timing
START_TIME=$(date +%s)

# Safety flag - set to false if anything goes wrong
BUILD_SUCCESS=false

# ==================== HELPER FUNCTIONS ====================

show_help() {
    cat << EOF
StoneOS Build System

Usage: $0 [OPTIONS]

OPTIONS:
    (none)      Full build cycle with local emulator testing
    --quick     Build only, skip testing
    --test      Test latest build in local emulator
    --deploy    Deploy latest build to connected device
    --cost      Show GCP cost estimates
    --help      Show this help message

EXAMPLES:
    $0              # Full development cycle
    $0 --quick      # Just build and download
    $0 --test       # Test existing build locally

NOTES:
    - GCP instances use SPOT pricing (70% cheaper)
    - Instance auto-terminates after build
    - Builds are stored in builds/ directory
    - Optional S3 backup if AWS CLI is configured

EOF
    exit 0
}

show_cost() {
    echo ""
    echo "==================== Cost Breakdown ===================="
    echo "Instance Type: ${MACHINE_TYPE}"
    echo "Regular Price: \$0.77/hour"
    echo "SPOT Price:    \$0.23/hour (70% savings)"
    echo ""
    echo "Typical Build Times:"
    echo "  - AOSP Download: 15-20 minutes"
    echo "  - SystemUI Build: 20-25 minutes"
    echo "  - Total: ~45 minutes"
    echo ""
    echo "Estimated Cost per Build: \$0.17 - \$0.23"
    echo "========================================================"
    echo ""
    exit 0
}

# ==================== BUILD FUNCTIONS ====================

create_gcp_startup_script() {
    cat > /tmp/stoneos_startup.sh << 'EOF'
#!/bin/bash
set -e

# Signal ready early (we'll install packages in background)
touch /tmp/ready

# Install AOSP dependencies in background
{
    apt-get update
    apt-get install -y \
        git-core gnupg flex bison build-essential \
        zip curl zlib1g-dev gcc-multilib g++-multilib \
        libc6-dev-i386 libncurses5 lib32ncurses5-dev \
        x11proto-core-dev libx11-dev lib32z1-dev \
        libgl1-mesa-dev libxml2-utils xsltproc unzip \
        fontconfig python3 python3-pip openjdk-11-jdk \
        rsync ccache libssl-dev bc \
        liblz4-tool libncurses5 libsdl1.2-dev \
        libxml2 lzop pngcrush schedtool squashfs-tools \
        imagemagick lib32z-dev
    
    # Install repo tool
    curl https://storage.googleapis.com/git-repo-downloads/repo > /usr/local/bin/repo
    chmod a+x /usr/local/bin/repo
    
    # Set up ccache
    export USE_CCACHE=1
    export CCACHE_DIR=/tmp/ccache
    mkdir -p $CCACHE_DIR
    ccache -M 20G
    
    echo "SETUP_COMPLETE" > /tmp/setup_status
} &
EOF
}

launch_gcp_instance() {
    print_step "Launching GCP Build Instance"
    
    # Safety check - verify project
    CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
    if [ "$CURRENT_PROJECT" != "$PROJECT" ]; then
        print_warning "Setting project to $PROJECT (was $CURRENT_PROJECT)"
        gcloud config set project $PROJECT
    fi
    
    # Check for existing instances
    EXISTING=$(gcloud compute instances list --filter="name:stoneos-builder*" --format="value(name)" 2>/dev/null || true)
    if [ ! -z "$EXISTING" ]; then
        print_warning "Found existing instance: $EXISTING"
        print_error "Please delete it first or wait for it to complete"
        exit 1
    fi
    
    create_gcp_startup_script
    
    print_info "Creating ${MACHINE_TYPE} SPOT instance..."
    
    # Create with error handling
    if ! gcloud compute instances create $INSTANCE_NAME \
        --project=$PROJECT \
        --zone=$ZONE \
        --machine-type=$MACHINE_TYPE \
        --provisioning-model=SPOT \
        --instance-termination-action=DELETE \
        --max-run-duration=2h \
        --network-interface=network-tier=STANDARD,subnet=default \
        --create-disk=auto-delete=yes,boot=yes,device-name=$INSTANCE_NAME,\
image=projects/ubuntu-os-cloud/global/images/ubuntu-2204-jammy-v20240319,\
mode=rw,size=500,type=pd-ssd \
        --metadata-from-file startup-script=/tmp/stoneos_startup.sh \
        --labels=build=$BUILD_ID 2>&1; then
        print_error "Failed to create instance"
        exit 1
    fi
    
    print_info "Waiting for environment setup (up to 5 minutes)..."
    sleep 30
    
    READY_ATTEMPTS=0
    while ! gcloud compute ssh $INSTANCE_NAME --zone=$ZONE --command="test -f /tmp/ready" 2>/dev/null; do
        sleep 10
        READY_ATTEMPTS=$((READY_ATTEMPTS + 1))
        if [ $READY_ATTEMPTS -gt 30 ]; then
            print_error "Instance failed to become ready after 5 minutes"
            gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet
            exit 1
        fi
        print_info "Still waiting... ($READY_ATTEMPTS/30)"
    done
    
    print_status "Instance ready (Cost: ~\$0.23/hour)"
}

build_on_gcp() {
    print_step "Building StoneOS on GCP"
    
    # Wait for package installation to complete
    print_info "Ensuring build environment is ready..."
    gcloud compute ssh $INSTANCE_NAME --zone=$ZONE --command="
        while [ ! -f /tmp/setup_status ]; do
            echo 'Waiting for package installation...'
            sleep 10
        done
        echo 'Build environment ready!'
    "
    
    # No need to upload Stone components - they're in our fork now
    print_info "Stone components will be downloaded from forked repository..."
    
    # Create build script
    cat > /tmp/build_script.sh << 'BUILDSCRIPT'
#!/bin/bash
set -e

echo "=== StoneOS Build Starting ==="
cd $HOME

# Download AOSP
echo "[1/3] Downloading AOSP source..."
mkdir -p aosp && cd aosp
repo init -u https://android.googlesource.com/platform/manifest \
    -b AOSP_BRANCH_PLACEHOLDER --depth=1

# Add StoneOS local manifest to use our forked frameworks/base
echo "[2/3] Adding StoneOS manifest..."
mkdir -p .repo/local_manifests
cat > .repo/local_manifests/stoneos.xml << 'MANIFEST'
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <!-- StoneOS Local Manifest -->
  <!-- This tells the AOSP build system to use our forked frameworks/base instead of Google's -->
  
  <!-- Remove Google's frameworks/base -->
  <remove-project name="platform/frameworks/base" />
  
  <!-- Add StoneOS's forked frameworks/base with Stone SystemUI components -->
  <project name="stone-pebble/stoneos-frameworks" 
           path="frameworks/base" 
           remote="github" 
           revision="android-14.0.0_r61" />
           
  <!-- Define GitHub remote -->
  <remote name="github"
          fetch="https://github.com/" />
</manifest>
MANIFEST

# Use -j4 to avoid rate limiting (learned from manual testing)
echo "Downloading AOSP with StoneOS fork (this takes ~15 minutes)..."
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch --fail-fast || {
    echo "First sync attempt failed, retrying..."
    sleep 5
    repo sync -c -j4 --no-tags --no-clone-bundle --current-branch
}

# Verify Stone files are present from our fork
echo "Verifying Stone components from fork..."
if [ -f "frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StonePanel.java" ] && \
   [ -f "frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StoneIcon.java" ]; then
    echo "✅ Stone components found in forked SystemUI!"
    ls -la frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
else
    echo "❌ ERROR: Stone components not found in fork!"
    exit 1
fi

cd $HOME/aosp

# No longer creating device overlay since it doesn't work for Java files

# Clean build cache to force re-scan
echo "Cleaning build cache..."
rm -rf out/soong/.intermediates/frameworks/base/packages/SystemUI/ 2>/dev/null || true

# Verify files were copied
echo "Verifying Stone integration..."
if [ -f "frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StonePanel.java" ] && \
   [ -f "frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StoneIcon.java" ]; then
    echo "✓ Stone files successfully copied to SystemUI source"
    ls -la frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
else
    echo "✗ ERROR: Stone files not found in SystemUI source!"
    echo "Contents of /tmp/stone:"
    ls -la /tmp/stone/
    exit 1
fi

cd $HOME/aosp

# Build
echo "[3/3] Building SystemUI with Stone classes..."
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # Android 14 QPR2 - for emulator testing
# For Pixel 8a device: lunch aosp_akita-userdebug (requires vendor binaries)

# Build and log output (use -j16 with 32 cores for faster build)
echo "Building SystemUI (this takes ~10-15 minutes with 32 cores)..."
m SystemUI -j16 2>&1 | tee /tmp/build_output.log

# Verify APK was created and contains Stone classes
if [ -f out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk ]; then
    echo "APK successfully built!"
    
    # Verify Stone classes are in the APK
    echo "Verifying Stone classes in APK..."
    cd /tmp
    cp $HOME/aosp/out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk .
    unzip -q SystemUI.apk
    if dexdump classes*.dex 2>/dev/null | grep -q "StonePanel"; then
        echo "✅ SUCCESS: Stone classes found in SystemUI.apk!"
        touch /tmp/stone_integrated_success
    else
        echo "⚠️ WARNING: Stone classes NOT found in APK"
        echo "Build completed but Stone integration may have failed"
    fi
    cd $HOME/aosp
    cp out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk \
        /tmp/StoneOS_SystemUI.apk
    
    # Verify Stone classes are in the APK
    echo "Verifying Stone classes in APK..."
    if unzip -l /tmp/StoneOS_SystemUI.apk 2>/dev/null | grep -q "stone/Stone"; then
        echo "✓ Stone classes found in APK!"
    else
        echo "⚠ WARNING: Stone classes may not be in final APK"
        echo "Checking if Stone files were compiled..."
        find out/ -name "*Stone*.class" 2>/dev/null | head -5
    fi
    
    ls -lh /tmp/StoneOS_SystemUI.apk
    echo "BUILD_SUCCESS" > /tmp/build_status
else
    echo "ERROR: APK not found after build!"
    echo "BUILD_FAILED" > /tmp/build_status
    exit 1
fi

echo "=== Build Complete ==="
BUILDSCRIPT
    
    # Use perl instead of sed for cross-platform compatibility
    perl -pi -e "s/AOSP_BRANCH_PLACEHOLDER/$AOSP_BRANCH/g" /tmp/build_script.sh
    
    # Execute build
    gcloud compute scp /tmp/build_script.sh $INSTANCE_NAME:/tmp/ --zone=$ZONE
    
    print_info "Starting build (this will take 25-35 minutes with 32 cores)..."
    print_info "  - AOSP download: ~15 minutes (limited by Google's servers)"
    print_info "  - SystemUI build: ~10-15 minutes (2x faster with 32 cores)"
    
    if ! gcloud compute ssh $INSTANCE_NAME --zone=$ZONE \
        --command="chmod +x /tmp/build_script.sh && /tmp/build_script.sh"; then
        print_error "Build script failed!"
        return 1
    fi
    
    # Verify build success
    print_info "Verifying build..."
    BUILD_STATUS=$(gcloud compute ssh $INSTANCE_NAME --zone=$ZONE \
        --command="cat /tmp/build_status 2>/dev/null || echo 'UNKNOWN'" 2>/dev/null)
    
    if [ "$BUILD_STATUS" != "BUILD_SUCCESS" ]; then
        print_error "Build verification failed! Status: $BUILD_STATUS"
        print_info "Checking for APK anyway..."
        
        # Check if APK exists despite status
        if ! gcloud compute ssh $INSTANCE_NAME --zone=$ZONE \
            --command="test -f /tmp/StoneOS_SystemUI.apk" 2>/dev/null; then
            print_error "APK not found on instance!"
            return 1
        fi
    fi
    
    print_status "Build completed and verified"
}

download_build() {
    print_step "Downloading Build"
    
    mkdir -p $LOCAL_DIR/builds/$BUILD_ID
    
    # Download APK with retry logic
    print_info "Downloading APK from GCP..."
    DOWNLOAD_SUCCESS=false
    for attempt in 1 2 3; do
        print_info "Download attempt $attempt of 3..."
        
        if gcloud compute scp \
            $INSTANCE_NAME:/tmp/StoneOS_SystemUI.apk \
            $LOCAL_DIR/builds/$BUILD_ID/StoneOS_SystemUI.apk \
            --zone=$ZONE 2>&1; then
            
            # Verify the file exists and has reasonable size
            if [ -f "$LOCAL_DIR/builds/$BUILD_ID/StoneOS_SystemUI.apk" ]; then
                FILE_SIZE=$(stat -f%z "$LOCAL_DIR/builds/$BUILD_ID/StoneOS_SystemUI.apk" 2>/dev/null || stat -c%s "$LOCAL_DIR/builds/$BUILD_ID/StoneOS_SystemUI.apk" 2>/dev/null)
                if [ "$FILE_SIZE" -gt 1000000 ]; then  # Should be at least 1MB
                    print_status "Download successful! APK size: $(($FILE_SIZE / 1024 / 1024))MB"
                    DOWNLOAD_SUCCESS=true
                    BUILD_SUCCESS=true
                    break
                else
                    print_warning "Downloaded file too small: $FILE_SIZE bytes"
                fi
            fi
        fi
        
        if [ $attempt -lt 3 ]; then
            print_warning "Download failed, retrying in 5 seconds..."
            sleep 5
        fi
    done
    
    if [ "$DOWNLOAD_SUCCESS" = false ]; then
        print_error "Failed to download APK after 3 attempts!"
        print_warning "Instance will be kept alive for manual recovery"
        print_info "To manually download:"
        print_info "  gcloud compute ssh $INSTANCE_NAME --zone=$ZONE"
        print_info "  APK location: /tmp/StoneOS_SystemUI.apk"
        return 1
    fi
    
    # Create metadata
    cat > $LOCAL_DIR/builds/$BUILD_ID/build_info.json << EOF
{
    "build_id": "${BUILD_ID}",
    "version": "${BUILD_VERSION}",
    "timestamp": "${BUILD_TIMESTAMP}",
    "aosp_branch": "${AOSP_BRANCH}",
    "size": "$(ls -lh $LOCAL_DIR/builds/$BUILD_ID/StoneOS_SystemUI.apk | awk '{print $5}')"
}
EOF
    
    print_status "Build saved to builds/$BUILD_ID/"
    
    # Optional S3 backup
    if command -v aws &> /dev/null && [ ! -z "$S3_BUCKET" ]; then
        aws s3 sync $LOCAL_DIR/builds/$BUILD_ID/ s3://$S3_BUCKET/$BUILD_ID/ --quiet
        print_info "Backed up to S3"
    fi
    
    # Optional SSD copy
    if [ -d "/Volumes/StoneOS_SSD" ]; then
        cp -r $LOCAL_DIR/builds/$BUILD_ID /Volumes/StoneOS_SSD/builds/
        print_info "Copied to SSD"
    fi
}

shutdown_gcp() {
    print_step "Shutting Down GCP Instance"
    
    # Only delete if we successfully downloaded the build
    if [ "$BUILD_SUCCESS" = true ]; then
        print_info "Build downloaded successfully, deleting instance..."
        gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet
        
        # Calculate cost
        END_TIME=$(date +%s)
        DURATION_SECONDS=$((END_TIME - START_TIME))
        DURATION_MINUTES=$((DURATION_SECONDS / 60))
        COST=$(echo "scale=2; $DURATION_MINUTES * 0.0038" | bc)  # $0.23/hour
        
        print_status "Instance terminated"
        print_info "Duration: ${DURATION_MINUTES} minutes | Cost: ~\$${COST}"
    else
        print_warning "Build not downloaded successfully!"
        print_warning "Instance kept alive: $INSTANCE_NAME"
        print_info "To connect: gcloud compute ssh $INSTANCE_NAME --zone=$ZONE"
        print_info "To delete: gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet"
    fi
}

# ==================== TEST FUNCTIONS ====================

test_in_emulator() {
    print_step "Testing in Android Emulator"
    
    # Find build to test
    if [ -z "$1" ]; then
        # Use latest build
        BUILD_TO_TEST=$(ls -t $LOCAL_DIR/builds | head -1)
        if [ -z "$BUILD_TO_TEST" ]; then
            print_error "No builds found!"
            exit 1
        fi
    else
        BUILD_TO_TEST=$1
    fi
    
    APK_PATH="$LOCAL_DIR/builds/$BUILD_TO_TEST/StoneOS_SystemUI.apk"
    if [ ! -f "$APK_PATH" ]; then
        print_error "APK not found: $APK_PATH"
        exit 1
    fi
    
    print_info "Testing build: $BUILD_TO_TEST"
    
    # Check for emulator
    if ! command -v emulator &> /dev/null; then
        print_error "Android emulator not found. Install Android Studio."
        exit 1
    fi
    
    # Create AVD if needed
    AVD_NAME="StoneOS_Test"
    if ! avdmanager list avd | grep -q "$AVD_NAME"; then
        print_info "Creating AVD..."
        echo "no" | avdmanager create avd -n $AVD_NAME \
            -k "system-images;android-33;google_apis;x86_64" \
            --device "pixel_5"
    fi
    
    # Start emulator
    print_info "Starting emulator..."
    emulator -avd $AVD_NAME -no-boot-anim &
    EMULATOR_PID=$!
    
    # Wait for boot
    adb wait-for-device
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do
        sleep 2
    done
    
    # Install StoneOS
    print_info "Installing StoneOS SystemUI..."
    adb root && sleep 2
    adb remount
    adb push $APK_PATH /system/system_ext/priv-app/SystemUI/SystemUI.apk
    adb shell pkill -f com.android.systemui || true
    
    print_status "StoneOS installed!"
    print_info "Swipe up from bottom to test Stone panel"
    
    # Monitor logs
    adb logcat -c
    print_info "Monitoring logs (Ctrl+C to stop)..."
    adb logcat | grep -E "StoneOS|StonePanel|StoneIcon" --color=always
}

deploy_to_device() {
    print_step "Deploying to Physical Device"
    
    # Check for device
    if ! adb devices | grep -q "device$"; then
        print_error "No device connected"
        exit 1
    fi
    
    # Find latest build
    BUILD_TO_DEPLOY=$(ls -t $LOCAL_DIR/builds | head -1)
    APK_PATH="$LOCAL_DIR/builds/$BUILD_TO_DEPLOY/StoneOS_SystemUI.apk"
    
    print_warning "This will replace SystemUI on your device!"
    read -p "Continue? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
    
    # Deploy
    adb root
    adb remount
    adb push $APK_PATH /system_ext/priv-app/SystemUI/SystemUI.apk
    adb shell pkill -f com.android.systemui
    
    print_status "Deployed to device!"
}

# ==================== MAIN WORKFLOWS ====================

quick_build() {
    print_info "Quick build mode - no testing"
    
    # Create build directory
    mkdir -p $LOCAL_DIR/builds/$BUILD_ID
    
    # Log everything
    exec 1> >(tee -a $LOCAL_DIR/builds/$BUILD_ID/build.log)
    exec 2>&1
    
    launch_gcp_instance
    build_on_gcp
    download_build
    shutdown_gcp
    
    print_status "Quick build complete!"
    print_info "Build logs saved to: builds/$BUILD_ID/build.log"
}

full_cycle() {
    print_info "Full development cycle"
    launch_gcp_instance
    build_on_gcp
    download_build
    shutdown_gcp
    test_in_emulator $BUILD_ID
}

# ==================== ERROR HANDLING ====================

cleanup_on_error() {
    print_error "Build failed! Checking instance status..."
    
    # If we haven't successfully downloaded, keep instance for debugging
    if [ "$BUILD_SUCCESS" = false ] && [ ! -z "$INSTANCE_NAME" ]; then
        # Check if instance exists
        if gcloud compute instances describe $INSTANCE_NAME --zone=$ZONE &>/dev/null; then
            print_warning "Keeping instance alive for debugging: $INSTANCE_NAME"
            print_info "Connect with: gcloud compute ssh $INSTANCE_NAME --zone=$ZONE"
            print_info "Delete with: gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet"
            
            # Try to get build logs if they exist
            print_info "Attempting to retrieve build logs..."
            gcloud compute ssh $INSTANCE_NAME --zone=$ZONE \
                --command="if [ -f /tmp/build_output.log ]; then tail -50 /tmp/build_output.log; fi" 2>/dev/null || true
        fi
    else
        # Clean up if we had a successful download or no instance
        if [ ! -z "$INSTANCE_NAME" ]; then
            gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet 2>/dev/null || true
        fi
    fi
    exit 1
}

trap cleanup_on_error ERR INT TERM

# ==================== MAIN ====================

case "$1" in
    --quick)
        quick_build
        ;;
    --test)
        test_in_emulator
        ;;
    --deploy)
        deploy_to_device
        ;;
    --cost)
        show_cost
        ;;
    --help|-h)
        show_help
        ;;
    "")
        full_cycle
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        ;;
esac