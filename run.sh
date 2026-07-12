#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
home_dir="${HOME:?}"
jdk21_home="$home_dir/apps/jdk-21.0.10"
android_home="${ANDROID_HOME:-$home_dir/apps/android/android-sdk-linux}"
adb="${ADB:-$android_home/platform-tools/adb}"
build_script="$root_dir/build.sh"
build_type="debug"
package_name="ltd.evilcorp.atox"
activity_name="ltd.evilcorp.atox.MainActivity"

# Usage:
#   ./run.sh            -> install and launch debug APK
#   ./run.sh release    -> install and launch signed release APK
# If the selected APK is missing, this script builds it first.

# Enforce Java 21 before building or installing.
java_home="${JAVA_HOME:-$jdk21_home}"

# Fail fast if the selected JDK is missing or is not Java 21.
if [[ ! -x "$java_home/bin/java" || ! -x "$java_home/bin/javac" ]]; then
  echo "No usable Java 21 installation found at: $java_home" >&2
  exit 1
fi

java_version="$("$java_home/bin/java" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
if [[ "$java_version" != 21* ]]; then
  echo "JAVA_HOME must point to Java 21. Found version '$java_version' at: $java_home" >&2
  exit 1
fi

if [[ ${1:-} == "debug" || ${1:-} == "release" ]]; then
  build_type="$1"
  shift
fi

# Default to the APK that matches the chosen build mode unless the caller overrides it.
apk_path="${1:-$root_dir/atox/build/outputs/apk/$build_type/atox-$build_type.apk}"
if [[ "$build_type" == "release" && $# -eq 0 ]]; then
  # Release installs use the signed artifact produced by build.sh.
  apk_path="$root_dir/atox/build/outputs/apk/release/atox-release-signed.apk"
fi

if [[ ! -x "$adb" ]]; then
  echo "adb not found at: $adb" >&2
  exit 1
fi

if [[ ! -f "$apk_path" ]]; then
  # Build on demand so `run.sh` works even after a clean checkout.
  if [[ ! -x "$build_script" ]]; then
    echo "Build script not found or not executable: $build_script" >&2
    exit 1
  fi
  "$build_script" "$build_type"
fi

# Pick the only attached device unless the caller already set ANDROID_SERIAL.
device_serial="${ANDROID_SERIAL:-}"
if [[ -z "$device_serial" ]]; then
  mapfile -t devices < <("$adb" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
  if [[ ${#devices[@]} -eq 0 ]]; then
    echo "No attached Android device found." >&2
    exit 1
  fi
  if [[ ${#devices[@]} -gt 1 ]]; then
    echo "Multiple devices attached. Set ANDROID_SERIAL to choose one:" >&2
    printf '  %s\n' "${devices[@]}" >&2
    exit 1
  fi
  device_serial="${devices[0]}"
fi

# Install the APK, then launch the main activity directly.
"$adb" -s "$device_serial" wait-for-device
"$adb" -s "$device_serial" install -r -d "$apk_path"
"$adb" -s "$device_serial" shell am start -n "$package_name/$activity_name" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
