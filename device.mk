#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# AAPT
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxxhdpi

# Alert slider
PRODUCT_PACKAGES += \
    KeyHandler \
    DeviceSettings \
    tri-state-key-calibrate

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/audio_effects_config.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/sku_sun/audio_effects_config.xml \
    $(LOCAL_PATH)/configs/audio/audio_module_config_primary.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio/audio_module_config_primary.xml \
    $(LOCAL_PATH)/configs/audio/audio_policy_volumes.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_volumes.xml \
    $(LOCAL_PATH)/configs/audio/default_volume_tables.xml:$(TARGET_COPY_OUT_VENDOR)/etc/default_volume_tables.xml

# Boot animation
TARGET_SCREEN_HEIGHT := 3168
TARGET_SCREEN_WIDTH := 1440

# Display
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/display/displayconfig.xml:$(TARGET_COPY_OUT_VENDOR)/etc/displayconfig/display_id_4630946756802996883.xml

# Keymint
PRODUCT_PACKAGES += \
    android.hardware.security.keymint3-service.strongbox.nxp \
    android.hardware.weaver-service.nxp

# Overlays
DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay-lineage

PRODUCT_PACKAGES += \
    EuiccPolicyResTarget \
    FrameworksResTargetEuicc \
    KeyHandlerResTarget \
    OPlusFrameworksResTarget \
    OPlusSettingsProviderResTarget \
    OPlusSettingsResTarget \
    OPlusSystemUIResTarget

# NFC
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nfc/libnfc-mtp-SN220.conf_23821:$(TARGET_COPY_OUT_ODM)/etc/libnfc-mtp-SN220.conf_23821 \
    $(LOCAL_PATH)/configs/nfc/libnfc-mtp-SN220.conf_23893:$(TARGET_COPY_OUT_ODM)/etc/libnfc-mtp-SN220.conf_23893 \
    $(LOCAL_PATH)/configs/nfc/libnfc-nci.conf:$(TARGET_COPY_OUT_VENDOR)/etc/libnfc-nci.conf

# PowerShare
PRODUCT_PACKAGES += \
    vendor.lineage.powershare-service.oplus

# Regional properties
REGIONAL_PROP_FILES := $(wildcard $(LOCAL_PATH)/properties/*/*.prop)

PRODUCT_COPY_FILES += $(foreach f,$(REGIONAL_PROP_FILES), \
    $(f):$(TARGET_COPY_OUT_ODM)/etc/$(patsubst $(LOCAL_PATH)/properties/%,%,$(f)) \
    $(f):$(TARGET_COPY_OUT_RECOVERY)/root/vendor/odm/etc/$(patsubst $(LOCAL_PATH)/properties/%,%,$(f)))

# SecureElement
PRODUCT_PACKAGES += \
    SecureElementResTarget_Vendor

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/permissions/com.android.se.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/com.android.se.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.se.omapi.ese.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.se.omapi.ese.xml \
    frameworks/native/data/etc/android.hardware.se.omapi.uicc.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.se.omapi.uicc.xml \

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# SuperVOOC charging ring
PRODUCT_PACKAGES += \
    libnativeChargingRing

# Telephony
PRODUCT_PACKAGES += \
    OplusEsimSwitcher \
    OplusEuicc

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.euicc.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/permissions/android.hardware.telephony.euicc.xml

# Touch
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/keylayout/touchpanel.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/touchpanel.kl

# Vibrator (YAAP sm8650-common style profiles)
# sm8750-common QTI HAL + dodge effect lib. Profiles via persist.sys.haptic_profile:
#   richtap | crisp | gentle | op13crisp (default) | op13gentle
# op13crisp/op13gentle = dodge stock def/soft from odm/etc/vibrator/9999/{def,soft}.
# Stock OOS uses def_style; op13gentle is the weaker soft_style.
$(call soong_config_set,qti_vibrator,effect_lib,libqtivibratoreffect.oplus.dodge)

PRODUCT_PACKAGES += \
    libqtivibratoreffect.oplus.dodge

PRODUCT_PRODUCT_PROPERTIES += \
    persist.sys.haptic_profile=op13crisp

# ProXDR / HDR photo pipeline.
#
# Read off stock CPH2653 16.0.9.402 with getprop; all four are absent here, which
# is why Gallery's ultra_hdr / local_hdr / hdr_vision_brighten / edr_listener
# feature checks stay false and photos render SDR even though the panel reports
# ro.surface_flinger.has_HDR_display=true.
#
# Note the panel advertises supportedHdrTypes=INVALID even on stock, so ProXDR
# does not ride the standard Android HDR types -- it is Oplus's own localhdr/uhdr
# path, gated purely on these props.
#
# Dolby Vision is deliberately NOT enabled here (stock also sets
# persist.sys.feature.dolby_vision{,_app} and ro.vendor.oplus.dolby_vision_dpu{,.dvs}).
# Dolby on this device is a separate, flash-verified port that lives on its own
# branch; turning the UI on without that backend just offers a broken toggle.
PRODUCT_PRODUCT_PROPERTIES += \
    persist.sys.feature.hdr_vision_app=1 \
    persist.sys.feature.localhdr_version=2 \
    persist.sys.feature.uhdr.support=true \
    persist.sys.feature.support.edrlistener=true

# Stock 11.F.94 perfd. Before sm8750-common so its perfconfigstore, which
# leaves sun scroll and launch hints off, does not replace the stock one.
$(call inherit-product, vendor/oneplus/dodge/qti-perfd/qti-perfd.mk)

# Inherit from the common OEM chipset makefile.
$(call inherit-product, device/oneplus/sm8750-common/common.mk)

# 16 GB Dalvik heap. common.mk installs the 6 GB profile as vendor props.
# Product build.prop is loaded after vendor and replaces those values.
$(call inherit-product, frameworks/native/build/phone-xhdpi-16384-dalvik-heap.mk)


# Real-time 1080p120: override the stock odm camera feature blob.
# This has to be declared *before* inheriting dodge-vendor.mk. inherit-product
# only appends an @inherit: marker that gets expanded after this file is parsed,
# so a filter-out here never sees (let alone removes) the vendor entries.
# Duplicate PRODUCT_COPY_FILES destinations are resolved first-one-wins, so
# listing ours ahead of the inherit is what actually makes the override stick.
#
# oplus_camera_config is deliberately NOT copied here any more. extract-files.py
# now decrypts it and sets the 120fps vendor tags directly on the vendor blob,
# and because this list wins first-one-wins, a static copy here would silently
# override those values with the older, narrower ones (main.only.support=1,
# zoom.range 1,3.5, max.zoom.list 1,2.9,10) and undo the patch.
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/camera/camera_unit_feature_config.protobuf:$(TARGET_COPY_OUT_ODM)/etc/camera/config/camera_unit_feature_config.protobuf

# Inherit from the proprietary files makefile.
$(call inherit-product, vendor/oneplus/dodge/dodge-vendor.mk)

# Camera
$(call inherit-product-if-exists, vendor/oplus/camera/opluscamera.mk)

# OplusCamera is system_ext + non-coredomain (vendor HAL/DSP/prop access).
PRODUCT_SELINUX_TREBLE_LABELING_TRACKING_LIST_FILE := \
    device/oneplus/dodge/sepolicy/tracking_list.yaml

# libcsextimpl.so NEEDs libcameraservice.so, which cameraserver only links
# statically, so nothing else installs a shared copy. Must be PRODUCT_PACKAGES
# only — Soong rejects shared_libs/required on libcsextimpl (AIDL V3 vs V4).
PRODUCT_PACKAGES += \
    libcameraservice

# fmt 11 for the OxygenOS libcsextimpl.so on Android 17 (see libshims/fmt11)
PRODUCT_PACKAGES += \
    libfmt11_shim

# Fusion light sensor (content-immune ALS). Dormant until
# persist.alpha.fusion_light=1 is set on device — see the repo README.
$(call inherit-product-if-exists, vendor/oplus/fusionlight/fusionlight.mk)

# OPLUS Face Unlock (UFF AIDL IFace). Do not combine with Paranoid Sense —
# FaceService treats ro.face.sense_service as exclusive. See
# vendor/oplus/opfaceunlock/README.md.
$(call inherit-product-if-exists, vendor/oplus/opfaceunlock/opfaceunlock.mk)
