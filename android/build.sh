#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
ndk="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/27.2.12479018}"
build="$root/build-android-arm64"
if [[ ! -d "$root/android/deps/SDL/.git" ]]; then
    git clone --depth 1 --branch release-2.32.10 https://github.com/libsdl-org/SDL.git "$root/android/deps/SDL"
fi
cmake -S "$root" -B "$build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-23 \
    -DANDROID_STL=c++_shared -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DCMAKE_BUILD_TYPE=Release -DFETCHCONTENT_TRY_FIND_PACKAGE_MODE=NEVER \
    -DFETCHCONTENT_SOURCE_DIR_SDL2="$root/android/deps/SDL" \
    -DSDL_SHARED=ON -DBUILD_SHARED_LIBS=ON -DBUILD_TESTS=OFF -DBUILD_EDITORS=OFF \
    -DNO_NETWORK=ON -DSDL2_FORCE_GLES=ON -DSMW_INSTALL_PORTABLE=ON
cmake --build "$build" --target smw --parallel "${BUILD_JOBS:-4}"
mkdir -p "$build/apk-libs/arm64-v8a"
find "$build" -path "$build/apk-libs" -prune -o -type f -name '*.so' \
    -exec cp '{}' "$build/apk-libs/arm64-v8a/" \;
cp "$ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$build/apk-libs/arm64-v8a/"
# Retain original build outputs for crash analysis; strip only the packaged copies.
"$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$build/apk-libs/arm64-v8a/"*.so
cd "$root/android"
./gradlew --no-daemon :app:assembleDebug
