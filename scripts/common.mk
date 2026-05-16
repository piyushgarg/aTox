SRCDIR			:= $(CURDIR)/_git
DESTDIR			:= $(CURDIR)/_install
BUILDDIR		:= $(CURDIR)/_build/$(TARGET)

export CFLAGS		:= -O3 -pipe
export CXXFLAGS		:= -O3 -pipe
export LDFLAGS		:=

export PATH		:= $(DESTDIR)/host/bin:$(PATH)
