#
# Copyright (C) 2021-2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from dodge device
$(call inherit-product, device/oneplus/dodge/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_dodge
PRODUCT_DEVICE := dodge
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := CPH2653

TARGET_HAS_UDFPS := true
TARGET_SUPPORTS_QUICK_TAP := true
TARGET_DISABLE_EPPE := true
WITH_GMS := true
TARGET_INCLUDE_LIVE_WALLPAPERS := true
TARGET_CUSTOM_UDFPS := true
SURFACE_FLINGER_BOOST := true

TARGET_SUPPORTED_REFRESH_RATES := 1,30,60,90,120
BYPASS_CHARGE_SUPPORTED := true

EVO_BUILD_TYPE := Official

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="qssi_64-user 16 BP2A.250605.015 1783088256304 release-keys" \
    BuildFingerprint=OnePlus/CPH2653EEA/OP5D55L1:16/BP2A.250605.015/V.R4T3.52da06f-2e397f6-2e81775:user/release-keys \
    DeviceName=OP5D55L1 \
    DeviceProduct=CPH2653 \
    SystemDevice=OP5D55L1 \
    SystemName=CPH2653

# AxionAOSP Flags
AXION_MAINTAINER := Kill3rEz

# Cameras
AXION_CAMERA_REAR_INFO := 50,50,50
AXION_CAMERA_FRONT_INFO := 32

# Processor
AXION_PROCESSOR := Snapdragon_8_Elite

# Enable blur.
# NOTE: TARGET_ENABLE_BLUR is not consumed anywhere in this tree, so it is inert
# on its own. SurfaceFlinger gates blur solely on this sysprop (defaults to 0),
# so the prop is what actually turns background blur on.
TARGET_ENABLE_BLUR := true
PRODUCT_SYSTEM_PROPERTIES += \
    ro.surface_flinger.supports_background_blur=1

# Enable AxionFX
TARGET_INCLUDE_AXFX := true

# Keep the display stack on Scudo (dual_allocator flag defaults ON in bionic).
# The Oplus gralloc/composer blobs bootloop under jemalloc, so opt this device
# out of DISPLAY_STACK_USE_JEMALLOC.
$(call soong_config_set,BIONIC,use_jemalloc_for_display_stack,false)