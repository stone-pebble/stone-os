#
# StoneOS x86_64 Product Configuration
#
# This product is based on aosp_x86_64 but with StoneOS customizations
#

# Inherit from AOSP x86_64 base
$(call inherit-product, build/target/product/aosp_x86_64.mk)

# Include StoneOS customizations
$(call inherit-product, vendor/stone/stoneos.mk)

# Override product information
PRODUCT_NAME := stoneos_x86_64
PRODUCT_DEVICE := generic_x86_64
PRODUCT_BRAND := StoneOS
PRODUCT_MODEL := StoneOS on x86_64
PRODUCT_MANUFACTURER := StoneOS
