# NDK_HOME and CMAKE should be passed from environment or Gradle
NDK_HOME ?= $(ANDROID_NDK_HOME)
ifeq ($(NDK_HOME),)
$(error ANDROID_NDK_HOME or NDK_HOME must be set)
endif

# Detect host OS for toolchain
HOST_OS := $(shell uname -s | tr '[:upper:]' '[:lower:]')
ifeq ($(HOST_OS),darwin)
    HOST_ARCH := darwin-x86_64
else
    HOST_ARCH := linux-x86_64
endif

DLLEXT		:= .so
TOOLCHAIN	:= $(NDK_HOME)/toolchains/llvm/prebuilt/$(HOST_ARCH)
SYSROOT		:= $(TOOLCHAIN)/sysroot
PREFIX		:= $(DESTDIR)/$(TARGET)
TOOLCHAIN_FILE	:= $(SRCDIR)/$(TARGET).cmake
TOOLCHAIN_CLANG_BIN	:= $(TOOLCHAIN)/bin/$(TARGET)$(NDK_API)
TOOLCHAIN_BIN	:= $(TOOLCHAIN)/bin/llvm
CMAKE ?= cmake

export CC		:= $(TOOLCHAIN_CLANG_BIN)-clang --sysroot=$(SYSROOT)
export CXX		:= $(TOOLCHAIN_CLANG_BIN)-clang++ --sysroot=$(SYSROOT)
export AR 		:= $(TOOLCHAIN_BIN)-ar
export RANLIB	:= $(TOOLCHAIN_BIN)-ranlib
export LD 		:= $(CC)
export AS		:= $(TOOLCHAIN_BIN)-as
export STRIP	:= $(TOOLCHAIN_BIN)-strip
export NM		:= $(TOOLCHAIN_BIN)-nm
export CFLAGS		:= -fPIC
export CXXFLAGS		:= -fPIC
export LDFLAGS		:= --sysroot=$(SYSROOT) -static-libstdc++
export PKG_CONFIG_LIBDIR:= $(PREFIX)/lib/pkgconfig
export PKG_CONFIG_PATH	:= $(PREFIX)/lib/pkgconfig
export PATH		:= $(TOOLCHAIN)/bin:$(PATH)


libsodium_CONFIGURE	:= --prefix=$(PREFIX) --host=$(TARGET) --with-sysroot=$(SYSROOT) --disable-shared CFLAGS="-fPIC" CXXFLAGS="-fPIC"
opus_CONFIGURE		:= --prefix=$(PREFIX) --host=$(TARGET) --with-sysroot=$(SYSROOT) --disable-shared CFLAGS="-fPIC" CXXFLAGS="-fPIC"
# TODO(robinlinden): Investigate how to get neon-asm working for armv7a again.
libvpx_CONFIGURE	:= --prefix=$(PREFIX) --libc=$(SYSROOT) --target=$(VPX_TARGET) --disable-examples --disable-unit-tests --enable-pic --disable-neon-asm --extra-cflags="--sysroot=$(SYSROOT) -fPIC" --extra-cxxflags="--sysroot=$(SYSROOT) -fPIC" --extra-ldflags="--sysroot=$(SYSROOT) -llog"
toxcore_CONFIGURE	:= -DCMAKE_INSTALL_PREFIX:PATH=$(PREFIX) -DCMAKE_TOOLCHAIN_FILE=$(TOOLCHAIN_FILE) -DANDROID_CPU_FEATURES=$(NDK_HOME)/sources/android/cpufeatures/cpu-features.c -DENABLE_STATIC=ON -DENABLE_SHARED=OFF -DCMAKE_POSITION_INDEPENDENT_CODE=ON

build: $(PREFIX)/toxcore.stamp

test: build
	@echo "No tests for Android builds"

$(NDK_HOME):
	@echo "Downloading NDK..."
	@$(PRE_RULE)
	@mkdir -p $(@D)
	test -f $(NDK_PACKAGE) || curl -s $(NDK_URL) -o $(NDK_PACKAGE)
	7z x $(NDK_PACKAGE) -o$(SRCDIR) > /dev/null
	@$(POST_RULE)

$(TOOLCHAIN_FILE): scripts/android.mk | $(NDK_HOME)
	@$(PRE_RULE)
	mkdir -p $(@D)
	echo 'set(CMAKE_SYSTEM_NAME Linux)' > $@
	echo >> $@
	echo 'set(CMAKE_BUILD_TYPE Release CACHE STRING "")' >> $@
	echo >> $@
	echo 'set(CMAKE_SYSROOT $(SYSROOT))' >> $@
	echo >> $@
	echo 'set(CMAKE_FIND_ROOT_PATH $(PREFIX))' >> $@
	echo 'set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)' >> $@
	echo 'set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)' >> $@
	echo 'set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)' >> $@
	echo 'set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)' >> $@
	@$(POST_RULE)

include scripts/release.mk
