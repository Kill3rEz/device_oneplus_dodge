#
# SPDX-FileCopyrightText: Paranoid Android
# SPDX-License-Identifier: Apache-2.0
#
# Ported from lineage_dodge.mk to the AOSPA (PenguinOS) product target.
#

ifeq (aospa_dodge,$(TARGET_PRODUCT))

# QCOM SoC platform. Needed in the PRODUCT phase because aospa-target.mk
# inherits device/qcom/common/common.mk, which requires TARGET_BOARD_PLATFORM
# to be defined already (the Lineage tree only sets it in BoardConfig).
TARGET_BOARD_PLATFORM := sun

# Build the kernel IN-TREE (like Axion), not via AOSPA's kernel-platform prebuilt model.
# Must be set in the product phase (before device/qcom/common inherits kernel-platform.mk,
# which defaults it to true and would then demand a prebuilt at device/qcom/sun-kernel/).
# Pairs with TARGET_KERNEL_LINEAGE_ONLY in device/oneplus/sm8750-common/BoardConfigCommon.mk.
TARGET_USES_KERNEL_PLATFORM := false

# The device ships the lineage-libperfmgr android.hardware.power HAL (see
# device/oneplus/sm8750-common/common.mk). Declare that we provide our own power
# HAL so device/qcom/common/common.mk skips inheriting
# vendor/qcom/opensource/power/power-vendor-product.mk, which would otherwise also
# add the QCOM android.hardware.power-service (+ power-v6.xml) and make VINTF fail
# with two providers of android.hardware.power IPower/default. Must be set here in
# the product phase, before aospa-target.mk -> device/qcom/common/common.mk.
TARGET_PROVIDES_POWERHAL := true

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/aosp_base_telephony.mk)

# Inherit from the device configuration.
$(call inherit-product, device/oneplus/dodge/device.mk)

# Inherit from the AOSPA configuration.
$(call inherit-product, vendor/aospa/target/product/aospa-target.mk)

# Product identity
PRODUCT_NAME := aospa_dodge
PRODUCT_DEVICE := dodge
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := CPH2653

# Device-specific feature flags (ported from lineage_dodge)
TARGET_HAS_UDFPS := true
TARGET_SUPPORTS_QUICK_TAP := true
TARGET_DISABLE_EPPE := true
TARGET_INCLUDE_LIVE_WALLPAPERS := true
TARGET_CUSTOM_UDFPS := true
SURFACE_FLINGER_BOOST := true
TARGET_SUPPORTED_REFRESH_RATES := 1,30,60,90,120
BYPASS_CHARGE_SUPPORTED := true

# Enable blur.
TARGET_USES_BLUR := true

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

# Spoof stock OxygenOS fingerprint (GMS/DDK)
PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="qssi_64-user 16 BP2A.250605.015 1783088256304 release-keys" \
    BuildFingerprint=OnePlus/CPH2653EEA/OP5D55L1:16/BP2A.250605.015/V.R4T3.52da06f-2e397f6-2e81775:user/release-keys \
    DeviceName=OP5D55L1 \
    DeviceProduct=CPH2653 \
    SystemDevice=OP5D55L1 \
    SystemName=CPH2653

endif
