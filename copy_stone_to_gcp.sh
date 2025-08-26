#!/bin/bash
# Script to copy Stone files to GCP instance and set up AOSP build

echo "Copying Stone files to GCP instance..."

# Create the files on the instance using cat
gcloud compute ssh aosp-build --zone=us-central1-a --command "cat > ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StonePanel.java << 'EOF'
$(cat /Users/samuellarson/Pebble/Github/stone-os/SystemUI/stone/StonePanel.java)
EOF"

gcloud compute ssh aosp-build --zone=us-central1-a --command "cat > ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/StoneIcon.java << 'EOF'
$(cat /Users/samuellarson/Pebble/Github/stone-os/SystemUI/stone/StoneIcon.java)
EOF"

echo "Files copied successfully!"