#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Partitions
BOARD_SUPER_PARTITION_SIZE := 13329498112

# Include the common OEM chipset BoardConfig.
include device/oneplus/sm8750-common/BoardConfigCommon.mk

DEVICE_PATH := device/oneplus/dodge

# Assert
# Both OnePlus 13 "dodge" SKUs (OP5D0DL1 = CN, OP5D55L1 = global) must be accepted.
# TARGET_OTA_ASSERT_DEVICE is passed through raw into the OTA metadata's `pre-device`,
# and the recovery splits that value on '|' (FINGERPRING_SEPARATOR in
# bootable/recovery install.cpp), NOT on comma. A comma list stays a single token and
# never matches ("Package is for product OP5D0DL1,OP5D55L1 but expected OP5D55L1"), so
# use '|' as the separator to keep BOTH SKUs flashable.
TARGET_OTA_ASSERT_DEVICE := OP5D0DL1|OP5D55L1

# Display
TARGET_SCREEN_DENSITY := 640

# Kernel
TARGET_KERNEL_ADDITIONAL_FLAGS += CONFIG_DODGE_DTB=y

# Properties
TARGET_ODM_PROP += $(DEVICE_PATH)/properties/odm.prop
TARGET_SYSTEM_EXT_PROP += $(DEVICE_PATH)/properties/system_ext.prop
TARGET_VENDOR_PROP += $(DEVICE_PATH)/properties/vendor.prop

# Recovery
TARGET_RECOVERY_UI_MARGIN_HEIGHT := 103

# Include the proprietary files BoardConfig.
include vendor/oneplus/dodge/BoardConfigVendor.mk

BUILD_BROKEN_VENDOR_PROPERTY_NAMESPACE := true

# Fusion light sensor
TARGET_USES_OPLUS_FUSIONLIGHT := true

# OPLUS Face Unlock (UFF AIDL IFace). Sense stays off — see alpha_dodge.mk.
TARGET_USES_OPLUS_FACEUNLOCK := true
# Feed props ON. Keep persist.alpha.fusion_light seeded 0 — enable with
# setprop after boot (never seed 1 until cold-boot proven).
TARGET_FUSIONLIGHT_ENABLE := true


# SEPolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor
BOARD_VENDOR_SEPOLICY_DIRS += vendor/oplus/fusionlight/sepolicy/vendor
BOARD_VENDOR_SEPOLICY_DIRS += vendor/oplus/opfaceunlock/sepolicy/vendor
# PenguinOS: use SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS (the var soong_config.mk:242 exports to
# soong's SystemExtPrivateSepolicyDirs), NOT BOARD_SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS, which is
# not consumed anywhere — so this device's sepolicy/private (refresh_rate_ext service_contexts +
# the system_server/app .te) was never compiled, and refresh_rate_ext fell back to
# default_android_service -> system_server addService denied -> FATAL -> bootloop.
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private
