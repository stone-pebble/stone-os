#!/bin/bash
# GCP AOSP Build Instance Management Script

PROJECT="dev-stone"
INSTANCE_NAME="aosp-build"
ZONE="us-central1-a"
MACHINE_TYPE="n2-standard-16"

# Create the instance with auto-shutdown
create_instance() {
    echo "Creating AOSP build instance..."
    gcloud compute instances create $INSTANCE_NAME \
        --project=$PROJECT \
        --zone=$ZONE \
        --machine-type=$MACHINE_TYPE \
        --network-interface=network-tier=STANDARD,subnet=default \
        --maintenance-policy=MIGRATE \
        --provisioning-model=STANDARD \
        --create-disk=auto-delete=yes,boot=yes,device-name=$INSTANCE_NAME,image=projects/ubuntu-os-cloud/global/images/ubuntu-2204-jammy-v20240319,mode=rw,size=500,type=pd-balanced \
        --metadata=startup-script='#!/bin/bash
            # Auto-shutdown after 8 hours
            echo "shutdown -h +480" | at now
            
            # Install AOSP dependencies
            apt update
            apt install -y git-core gnupg flex bison build-essential \
                zip curl zlib1g-dev gcc-multilib g++-multilib \
                libc6-dev-i386 libncurses5 lib32ncurses5-dev \
                x11proto-core-dev libx11-dev lib32z1-dev \
                libgl1-mesa-dev libxml2-utils xsltproc unzip \
                fontconfig python3 python3-pip openjdk-11-jdk \
                rsync ccache libssl-dev bc
            
            # Install repo tool
            mkdir -p /usr/local/bin
            curl https://storage.googleapis.com/git-repo-downloads/repo > /usr/local/bin/repo
            chmod a+x /usr/local/bin/repo
            
            echo "AOSP build environment ready!"
        ' \
        --labels=purpose=aosp-build,auto-shutdown=8hours
    
    echo "✅ Instance created with 8-hour auto-shutdown!"
}

# Start the instance
start_instance() {
    echo "Starting instance..."
    gcloud compute instances start $INSTANCE_NAME --zone=$ZONE
    echo "✅ Instance started"
}

# Stop the instance
stop_instance() {
    echo "Stopping instance..."
    gcloud compute instances stop $INSTANCE_NAME --zone=$ZONE
    echo "✅ Instance stopped (no charges while stopped, only disk storage)"
}

# Delete the instance completely
delete_instance() {
    echo "Deleting instance and disk..."
    gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE --quiet
    echo "✅ Instance deleted (no more charges)"
}

# Check instance status
check_status() {
    echo "Instance status:"
    gcloud compute instances list --filter="name=$INSTANCE_NAME" \
        --format="table(name,status,machineType.scope():label=TYPE,
                       networkInterfaces[].networkIP:label=INTERNAL_IP,
                       networkInterfaces[].accessConfigs[0].natIP:label=EXTERNAL_IP)"
}

# SSH into instance
ssh_to_instance() {
    gcloud compute ssh $INSTANCE_NAME --zone=$ZONE
}

# Get cost estimate
cost_estimate() {
    echo "==================================="
    echo "COST BREAKDOWN for $MACHINE_TYPE:"
    echo "==================================="
    echo "Hourly: ~$0.77"
    echo "Daily (24h): ~$18.48"
    echo "8-hour build: ~$6.16"
    echo ""
    echo "Disk (500GB): ~$0.08/hour while instance exists"
    echo ""
    echo "⚠️  IMPORTANT: Instance auto-shuts down after 8 hours!"
    echo "⚠️  Remember to DELETE instance when done to avoid disk charges!"
    echo "==================================="
}

# Main menu
case "$1" in
    create)
        create_instance
        ;;
    start)
        start_instance
        ;;
    stop)
        stop_instance
        ;;
    delete)
        delete_instance
        ;;
    status)
        check_status
        ;;
    ssh)
        ssh_to_instance
        ;;
    cost)
        cost_estimate
        ;;
    *)
        echo "Usage: $0 {create|start|stop|delete|status|ssh|cost}"
        echo ""
        echo "  create - Create new AOSP build instance"
        echo "  start  - Start stopped instance"
        echo "  stop   - Stop instance (keeps disk)"
        echo "  delete - Delete instance and disk (no more charges)"
        echo "  status - Check if instance is running"
        echo "  ssh    - SSH into instance"
        echo "  cost   - Show cost estimates"
        ;;
esac