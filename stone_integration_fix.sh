#!/bin/bash

# Fix for Stone classes not being included in SystemUI build
# This script modifies Android.bp to include our Stone classes

echo "=== Stone Integration Fix ==="
echo "This script adds Stone classes to SystemUI's Android.bp"

cat << 'EOF' > /tmp/android_bp_patch.txt
# Add to SystemUI's Android.bp srcs section:

# Find the srcs: [ section and add our files
# In frameworks/base/packages/SystemUI/Android.bp

# Look for:
#     srcs: [
#         "src/**/*.java",
#         "src/**/*.kt",

# Change to:
#     srcs: [
#         "src/**/*.java",
#         "src/**/*.kt",
#         "src/com/android/systemui/stone/*.java",

EOF

echo "Manual steps for next build:"
echo "1. After copying Stone files, edit Android.bp:"
echo "   vim frameworks/base/packages/SystemUI/Android.bp"
echo ""
echo "2. Find the 'srcs: [' section (around line 150-200)"
echo ""
echo "3. Add this line after 'src/**/*.kt':"
echo '   "src/com/android/systemui/stone/*.java",'
echo ""
echo "4. Save and build"

echo ""
echo "OR add this to the build script automatically:"
cat << 'AUTOPATCH'

# Add after copying Stone files:
echo "Patching Android.bp to include Stone classes..."
cd $HOME/aosp/frameworks/base/packages/SystemUI/

# Backup original
cp Android.bp Android.bp.backup

# Add Stone sources to build
sed -i '/srcs: \[/,/\]/ {
    /"src\/\*\*\/\*.kt",/a\
        "src/com/android/systemui/stone/*.java",
}' Android.bp

echo "Android.bp patched to include Stone classes"

AUTOPATCH