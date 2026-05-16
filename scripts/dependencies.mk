PRE_RULE = (echo "=== Building $@ ==="; ls -ld $@; true) && ls -ld $+
POST_RULE = ls -ld $@

#############################################################################
# libsodium

$(SRCDIR)/libsodium:
	mkdir -p $@
	curl -L https://github.com/jedisct1/libsodium/archive/refs/tags/$(LIBSODIUM_VERSION).tar.gz | tar -xzp --strip-components=1 -C $@

$(PREFIX)/libsodium.stamp: $(SRCDIR)/libsodium $(TOOLCHAIN_FILE)
	@$(PRE_RULE)
	mkdir -p $(BUILDDIR)/libsodium
	cd $(BUILDDIR)/libsodium && $(SRCDIR)/libsodium/configure --host=$(TARGET) --prefix=$(PREFIX) --disable-shared --enable-static CC="$(CC)" CXX="$(CXX)" AR="$(AR)" AS="$(AS)" RANLIB="$(RANLIB)" STRIP="$(STRIP)" CFLAGS="$(CFLAGS)" CPPFLAGS="$(CPPFLAGS)" LDFLAGS="$(LDFLAGS)"
	$(MAKE) -C $(BUILDDIR)/libsodium install
	mkdir -p $(@D) && touch $@
	@$(POST_RULE)

#############################################################################
# opus

$(SRCDIR)/opus:
	mkdir -p $@
	curl -L https://downloads.xiph.org/releases/opus/opus-$(OPUS_VERSION).tar.gz | tar -xzp --strip-components=1 -C $@

$(PREFIX)/opus.stamp: $(SRCDIR)/opus $(TOOLCHAIN_FILE)
	@$(PRE_RULE)
	mkdir -p $(BUILDDIR)/opus
	cd $(BUILDDIR)/opus && $(SRCDIR)/opus/configure --host=$(TARGET) --prefix=$(PREFIX) --disable-shared --enable-static CC="$(CC)" CXX="$(CXX)" AR="$(AR)" AS="$(AS)" RANLIB="$(RANLIB)" STRIP="$(STRIP)" CFLAGS="$(CFLAGS)" CPPFLAGS="$(CPPFLAGS)" LDFLAGS="$(LDFLAGS)"
	$(MAKE) -C $(BUILDDIR)/opus install
	mkdir -p $(@D) && touch $@
	@$(POST_RULE)

#############################################################################
# libvpx

$(SRCDIR)/libvpx:
	mkdir -p $@
	curl -L https://github.com/webmproject/libvpx/archive/refs/tags/$(LIBVPX_VERSION).tar.gz | tar -xzp --strip-components=1 -C $@

$(PREFIX)/libvpx.stamp: $(SRCDIR)/libvpx $(TOOLCHAIN_FILE)
	@$(PRE_RULE)
	mkdir -p $(BUILDDIR)/libvpx
	cd $(BUILDDIR)/libvpx && $(SRCDIR)/libvpx/configure --prefix=$(PREFIX) --libc=$(SYSROOT) --target=$(VPX_TARGET) --disable-examples --disable-unit-tests --enable-pic --disable-neon-asm --extra-cflags="--sysroot=$(SYSROOT)" --extra-cxxflags="--sysroot=$(SYSROOT)"
	$(MAKE) -C $(BUILDDIR)/libvpx install
	mkdir -p $(@D) && touch $@
	@$(POST_RULE)

#############################################################################
# toxcore

$(SRCDIR)/toxcore:
	mkdir -p $@
	curl -L https://github.com/TokTok/c-toxcore/archive/refs/tags/$(TOXCORE_VERSION).tar.gz | tar -xzp --strip-components=1 -C $@
	mkdir -p $@/third_party/cmp
	curl -L https://github.com/TokTok/cmp/archive/master.tar.gz | tar -xzp --strip-components=1 -C $@/third_party/cmp
	sed 's/REQUIRED//g' $@/cmake/Dependencies.cmake > $@/cmake/Dependencies.cmake.tmp && mv $@/cmake/Dependencies.cmake.tmp $@/cmake/Dependencies.cmake

$(PREFIX)/toxcore.stamp: $(SRCDIR)/toxcore $(TOOLCHAIN_FILE) $(foreach i,libsodium opus libvpx,$(PREFIX)/$i.stamp)
	@$(PRE_RULE)
	mkdir -p $(BUILDDIR)/toxcore
	cd $(BUILDDIR)/toxcore && $(CMAKE) $(SRCDIR)/toxcore -DCMAKE_INSTALL_PREFIX:PATH=$(PREFIX) -DCMAKE_TOOLCHAIN_FILE=$(TOOLCHAIN_FILE) -DANDROID_CPU_FEATURES=$(NDK_HOME)/sources/android/cpufeatures/cpu-features.c -DENABLE_STATIC=ON -DENABLE_SHARED=OFF -DMUST_BUILD_TOXAV=ON -DBOOTSTRAP_DAEMON=OFF -DLIBSODIUM_LIBRARIES=$(PREFIX)/lib/libsodium.a -DLIBSODIUM_INCLUDE_DIRS=$(PREFIX)/include -DOPUS_LIBRARIES=$(PREFIX)/lib/libopus.a -DOPUS_INCLUDE_DIRS=$(PREFIX)/include/opus -DVPX_LIBRARIES=$(PREFIX)/lib/libvpx.a -DVPX_INCLUDE_DIRS=$(PREFIX)/include -DLIBSODIUM_FOUND=ON -DOPUS_FOUND=ON -DVPX_FOUND=ON -DBUILD_TESTING=OFF
	$(MAKE) -C $(BUILDDIR)/toxcore toxcore_static
	mkdir -p $(PREFIX)/lib $(PREFIX)/include/tox
	cp $(BUILDDIR)/toxcore/libtoxcore.a $(PREFIX)/lib/
	cp $(SRCDIR)/toxcore/toxcore/*.h $(PREFIX)/include/tox/
	cp $(SRCDIR)/toxcore/toxav/*.h $(PREFIX)/include/tox/
	cp $(SRCDIR)/toxcore/toxencryptsave/*.h $(PREFIX)/include/tox/
	mkdir -p $(@D) && touch $@
	@$(POST_RULE)
