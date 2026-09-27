#!/bin/bash
# Run by apply-dodge-ports-evo.sh from inside vendor/oplus/camera/camera, after the *.patch files.
#
# libcsextimpl.so (OxygenOS 16 blob) needs 9 fmt::v11 symbols that Android 16 libbase exported;
# Android 17 libbase exports fmt::v12 instead, so cameraserver's dlopen fails
# ("cannot locate symbol _ZN3fmt3v116detail11assert_failEPKciS3_") and the OEM camera layer
# rejects configure_streams (no com.oplus.packageName vendor tag). Add libfmt11_shim.so
# (device/oneplus/dodge/libshims/fmt11) to its DT_NEEDED. Idempotent.
# Exit: 0 = applied, 3 = already applied, anything else = failure.
set -e
TOP="${1:?usage: $0 <source-top>}"
PATCHELF="$TOP/prebuilts/extract-tools/linux-x86/bin/patchelf-0_17_2"
LIB=proprietary/system_ext/lib64/libcsextimpl.so
SHIM=libfmt11_shim.so

[ -x "$PATCHELF" ] || { echo "missing $PATCHELF" >&2; exit 1; }
# An LFS pointer is not an ELF: fail loudly instead of patching garbage.
[ "$(head -c 4 "$LIB" | od -An -c | tr -d ' ')" = '177ELF' ] || { echo "$LIB is not an ELF (LFS pointer?)" >&2; exit 1; }
"$PATCHELF" --print-needed "$LIB" | grep -qx "$SHIM" && exit 3
"$PATCHELF" --add-needed "$SHIM" "$LIB"
"$PATCHELF" --print-needed "$LIB" | grep -qx "$SHIM"
