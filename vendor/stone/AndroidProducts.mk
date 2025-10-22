#
# StoneOS Product Definitions
#
# This file tells the Android build system about StoneOS products
#

PRODUCT_MAKEFILES := \
    $(LOCAL_DIR)/stoneos_x86_64.mk \

COMMON_LUNCH_CHOICES := \
    stoneos_x86_64-eng \
    stoneos_x86_64-userdebug \
    stoneos_x86_64-user \
