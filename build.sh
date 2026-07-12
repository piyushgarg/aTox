#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
gradle_wrapper="$root_dir/gradlew"
home_dir="${HOME:?}"
build_type="debug"
mode_selected="false"
jdk21_home="$home_dir/apps/jdk-21.0.10"
sign_apk_script="$home_dir/.mitmproxy/sign-apk.sh"
release_signing_dir="$root_dir/.release-signing"

# Usage:
#   ./build.sh            -> debug build
#   ./build.sh release    -> release build
#   ./build.sh clean      -> remove local build outputs and caches
usage() {
  cat <<EOF
Usage:
  ./build.sh [debug|release] [gradle-args...]
  ./build.sh clean
  ./build.sh --help

Defaults to a debug assemble when no mode is given.
Clean removes generated build output, local Gradle state, and native caches in this repo.
EOF
}

if [[ ${1:-} == "--help" || ${1:-} == "-h" ]]; then
  usage
  exit 0
fi

if [[ ${1:-} == "clean" ]]; then
  # Remove generated outputs plus the local Gradle/native caches this repo uses.
  rm -rf \
    "$root_dir/build" \
    "$root_dir/atox/build" \
    "$root_dir/core/build" \
    "$root_dir/domain/build" \
    "$root_dir/_build" \
    "$root_dir/_install" \
    "$root_dir/.gradle-home" \
    "$root_dir/.tmp" \
    "$root_dir/.release-signing"
  echo "Cleaned build outputs and local caches."
  exit 0
fi

if [[ ${1:-} == "debug" || ${1:-} == "release" ]]; then
  build_type="$1"
  mode_selected="true"
  shift
fi

# Enforce Java 21 for both the local build and Gradle toolchain discovery.
java_home="$jdk21_home"
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" && -x "$JAVA_HOME/bin/javac" ]]; then
  java_version="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
  if [[ "$java_version" == 21* ]]; then
    java_home="$JAVA_HOME"
  fi
fi

# Fail fast if the selected JDK is missing or is not Java 21.
if [[ ! -x "$java_home/bin/java" || ! -x "$java_home/bin/javac" ]]; then
  echo "No usable Java 21 installation found at: $java_home" >&2
  exit 1
fi

java_version="$("$java_home/bin/java" -version 2>&1 | awk -F '"' '/version/ { print $2; exit }')"
if [[ "$java_version" != 21* ]]; then
  echo "Java 21 required. Found version '$java_version' at: $java_home" >&2
  exit 1
fi

android_home="${ANDROID_HOME:-$home_dir/apps/android/android-sdk-linux}"
android_sdk_root="${ANDROID_SDK_ROOT:-$android_home}"
gradle_user_home="${GRADLE_USER_HOME:-$root_dir/.gradle-home}"
tmp_root="${TMPDIR_ROOT:-$root_dir/.tmp}"
ndk_source="${ANDROID_NDK_SOURCE:-$android_home/android-ndk-r29}"
if [[ -z "${ANDROID_NDK_SOURCE:-}" ]]; then
  # Prefer the SDK-root NDK 30 install, then fall back to legacy layouts.
  for candidate in \
    "$android_home/android-ndk-r30" \
    "$android_home/ndk/30.0.14904198" \
    "$android_home/android-ndk-r29"
  do
    if [[ -e "$candidate" ]]; then
      ndk_source="$candidate"
      break
    fi
  done
fi
if [[ "$mode_selected" == "true" || $# -eq 0 ]]; then
  # Keep the default path simple: mode selects assembleDebug/assembleRelease.
  gradle_task=("assemble${build_type^}")
  if [[ $# -gt 0 ]]; then
    gradle_task+=("$@")
  fi
else
  gradle_task=("$@")
fi

mkdir -p "$gradle_user_home" "$tmp_root"

# Pin Gradle's toolchain discovery to the same Java 21 install we validated.
cat > "$gradle_user_home/gradle.properties" <<EOF
org.gradle.java.installations.paths=$jdk21_home,$home_dir/apps/jdk-17.0.18
org.gradle.java.installations.fromEnv=JAVA_HOME
org.gradle.java.installations.auto-detect=true
org.gradle.java.installations.auto-download=false
EOF

if [[ ! -d "$ndk_source" ]]; then
  echo "Android NDK source not found at: $ndk_source" >&2
  exit 1
fi

# The native dependency scripts expect populated source trees under `_git`.
ensure_populated_source() {
  local source_dir="$1"
  local expected_path="$2"

  if [[ -d "$source_dir" && ! -e "$source_dir/$expected_path" ]]; then
    rm -rf "$source_dir"
  fi
}

ensure_populated_source "$root_dir/_git/libsodium" "configure"
ensure_populated_source "$root_dir/_git/opus" "configure"
ensure_populated_source "$root_dir/_git/libvpx" "configure"
ensure_populated_source "$root_dir/_git/toxcore" "cmake/Dependencies.cmake"

# Force toxcore to reconfigure when the Android toolchain changes.
rm -rf "$root_dir/_build/aarch64-linux-android/toxcore" "$root_dir/_install/aarch64-linux-android/toxcore.stamp"

# Export the build environment expected by Gradle and the native scripts.
export JAVA_HOME="$java_home"
export PATH="$java_home/bin:$PATH"
export ANDROID_HOME="$android_home"
export ANDROID_SDK_ROOT="$android_sdk_root"
export ANDROID_NDK_HOME="$ndk_source"
export GRADLE_USER_HOME="$gradle_user_home"
export GRADLE_DAEMON_BIND_ADDRESS="${GRADLE_DAEMON_BIND_ADDRESS:-127.0.0.1}"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$tmp_root"

"$gradle_wrapper" "${gradle_task[@]}"

if [[ "$build_type" == "release" ]]; then
  release_apk="$root_dir/atox/build/outputs/apk/release/atox-release.apk"
  signed_release_apk="$root_dir/atox/build/outputs/apk/release/atox-release-signed.apk"
  if [[ ! -x "$sign_apk_script" ]]; then
    echo "Release APK signing helper not found or not executable: $sign_apk_script" >&2
    exit 1
  fi
  if [[ ! -f "$release_apk" ]]; then
    echo "Release APK not found at: $release_apk" >&2
    exit 1
  fi
  mkdir -p "$release_signing_dir"
  signing_key="$release_signing_dir/release-signing.key"
  signing_cert="$release_signing_dir/release-signing.pem"
  signing_keystore="$release_signing_dir/release-signing.p12"
  if [[ ! -f "$signing_key" || ! -f "$signing_cert" ]]; then
    cat > "$release_signing_dir/openssl.cnf" <<'EOF'
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no
default_md = sha256

[req_distinguished_name]
CN = aTox Release Signing
O = aTox
OU = aTox
C = US

[v3_req]
keyUsage = critical, digitalSignature, nonRepudiation
extendedKeyUsage = codeSigning
subjectKeyIdentifier = hash
basicConstraints = critical, CA:FALSE
EOF
    openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
      -keyout "$signing_key" \
      -out "$signing_cert" \
      -config "$release_signing_dir/openssl.cnf"
  fi
  # Create the signed release artifact next to the unsigned APK.
  CERT_FILE="$signing_cert" \
  KEY_FILE="$signing_key" \
  KEYSTORE="$signing_keystore" \
  "$sign_apk_script" "$release_apk"
  if [[ -f "$signed_release_apk" ]]; then
    echo "Signed release APK: $signed_release_apk"
  fi
fi
