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

#Lunaris Stuff
LUNARIS_BUILD_TYPE := OFFICIAL
TARGET_HAS_UDFPS := true
TARGET_SUPPORTS_QUICK_TAP := true
TARGET_DISABLE_EPPE := true
WITH_GMS := true
TARGET_INCLUDE_LIVE_WALLPAPERS := true
TARGET_CUSTOM_UDFPS := true
SURFACE_FLINGER_BOOST := true

TARGET_SUPPORTED_REFRESH_RATES := 60,90,120
BYPASS_CHARGE_SUPPORTED := true

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="qssi_64-user 16 BP2A.250605.015 1780491741931 release-keys" \
    BuildFingerprint=OnePlus/CPH2653EEA/OP5D55L1:16/BP2A.250605.015/V.R4T3.26073a0-1df561-1f78cb:user/release-keys \
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

# Enable blur
TARGET_ENABLE_BLUR := true

# Enable AxionFX
TARGET_INCLUDE_AXFX := true