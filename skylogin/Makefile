OPENSSL_TARGET = openssl/target

SKYLOGIN_DIR = skylogin
SKYLOGIN_SRC = $(SKYLOGIN_DIR)/src

SOURCES = $(SKYLOGIN_SRC)/skyloginconnector.c $(SKYLOGIN_SRC)/common.c $(SKYLOGIN_SRC)/crc.c $(SKYLOGIN_SRC)/login.c $(SKYLOGIN_SRC)/objects.c $(SKYLOGIN_SRC)/random.c $(SKYLOGIN_SRC)/skylogin.c $(SKYLOGIN_SRC)/uic.c $(SKYLOGIN_SRC)/credentials.c
SOURCES_WINDOWS = $(SOURCES) $(SKYLOGIN_SRC)/platform_w32.c
SOURCES_LINUX = $(SOURCES) $(SKYLOGIN_SRC)/platform_unix.c

TARGET_DIR = target

JAVA_DIR = java

CFLAGS = -I$(SKYLOGIN_DIR)/include -Iopenssl/include -I$(JAVA_DIR) -O3 -D NDEBUG
CONFIGURE_OPTIONS = no-shared threads

all: linux32 linux64 windows32 windows64

$(OPENSSL_TARGET)/l32libcrypto.a:
	mkdir -p $(OPENSSL_TARGET)
	make -C openssl clean
	cd openssl && ./Configure linux-generic32 -m32 -fPIC $(CONFIGURE_OPTIONS) && cd ..
	$(MAKE) -C openssl depend build_crypto
	cp openssl/libcrypto.a $@

$(OPENSSL_TARGET)/l64libcrypto.a:
	mkdir -p $(OPENSSL_TARGET)
	make -C openssl clean
	cd openssl && ./Configure linux-x86_64 -fPIC $(CONFIGURE_OPTIONS) && cd ..
	$(MAKE) -C openssl depend build_crypto
	cp openssl/libcrypto.a $@

$(OPENSSL_TARGET)/w32libcrypto.a:
	mkdir -p $(OPENSSL_TARGET)
	make -C openssl clean
	cd openssl && ./Configure --cross-compile-prefix=i686-w64-mingw32- mingw -m32 $(CONFIGURE_OPTIONS) && cd ..
	$(MAKE) -C openssl depend build_crypto
	cp openssl/libcrypto.a $@

$(OPENSSL_TARGET)/w64libcrypto.a:
	mkdir -p $(OPENSSL_TARGET)
	make -C openssl clean
	cd openssl && ./Configure --cross-compile-prefix=x86_64-w64-mingw32- mingw64 $(CONFIGURE_OPTIONS) && cd ..
	$(MAKE) -C openssl depend build_crypto
	cp openssl/libcrypto.a $@

$(TARGET_DIR)/linux32/libskylogin.so: $(OPENSSL_TARGET)/l32libcrypto.a
	mkdir -p $(TARGET_DIR)/linux32
	gcc -m32 $(CFLAGS) -I$(JAVA_DIR)/linux -fPIC -o $@ $(SOURCES_LINUX) $^ -shared

$(TARGET_DIR)/linux64/libskylogin.so: $(OPENSSL_TARGET)/l64libcrypto.a
	mkdir -p $(TARGET_DIR)/linux64
	gcc $(CFLAGS) -I$(JAVA_DIR)/linux -fPIC -o $@ $(SOURCES_LINUX) $^ -shared

$(TARGET_DIR)/windows32/skylogin.dll: $(OPENSSL_TARGET)/w32libcrypto.a
	mkdir -p $(TARGET_DIR)/windows32
	i686-w64-mingw32-gcc -m32 $(CFLAGS) -I$(JAVA_DIR)/win32 -o $@ $(SOURCES_WINDOWS) $^ -shared -lws2_32 -lgdi32 -lrpcrt4

$(TARGET_DIR)/windows64/skylogin.dll: $(OPENSSL_TARGET)/w64libcrypto.a
	mkdir -p $(TARGET_DIR)/windows64
	x86_64-w64-mingw32-gcc $(CFLAGS) -I$(JAVA_DIR)/win32 -o $@ $(SOURCES_WINDOWS) $^ -shared -lws2_32 -lgdi32 -lrpcrt4

linux32: $(TARGET_DIR)/linux32/libskylogin.so
linux64: $(TARGET_DIR)/linux64/libskylogin.so
windows32: $(TARGET_DIR)/windows32/skylogin.dll
windows64: $(TARGET_DIR)/windows64/skylogin.dll

clean:
	rm -rf $(TARGET_DIR) $(SKYLOGIN_SRC)/*.o $(OPENSSL_TARGET)/*
	make -C openssl clean

.PHONY: all clean linux32 linux64 windows32 windows64
