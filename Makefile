PACKAGE ?= com.mniip.bananapeel
TARGET := bananapeel.apk

AAPT ?= $(SDK_BUILD_TOOLS)aapt
DEX ?= $(SDK_BUILD_TOOLS)dx
ZIP_ALIGN ?= $(SDK_BUILD_TOOLS)zipalign
ADB ?= adb
JAVAC ?= javac
APK_BUILDER ?= java -cp $(SDK_PATH)/tools/lib/sdklib.jar com.android.sdklib.build.ApkBuilderMain
JAR_SIGNER ?= jarsigner

PACKAGE_PATH := $(subst .,/,$(PACKAGE))

ANDROID_TARGET_SDK ?= 9
ANDROID_FRAMEWORK := $(SDK_PATH)/platforms/android-$(ANDROID_TARGET_SDK)/android.jar

MANIFEST := application/src/main/AndroidManifest.xml
RES_DIR := application/src/main/res/
RES_APK_FILE := $(patsubst %.apk,%-res.apk,$(TARGET))

R_JAVA_DIR := gen/
R_JAVA_FILE := $(R_JAVA_DIR)/$(PACKAGE_PATH)/R.java

LIB_JARS ?=
LIB_AARS ?=

LINKED_JARS := $(LIB_JARS) $(patsubst %.aar,%.aar/classes.jar,$(subst /,_,$(LIB_AARS))) $(wildcard $(patsubst %.aar,%.aar/libs/*.jar,$(subst /,_,$(LIB_AARS))))

CLASS_PATH := $(ANDROID_FRAMEWORK):$(subst $(eval) $(eval),:,$(LINKED_JARS))
JAVAC_FLAGS := -target 1.7 -source 1.7 $(MY_JAVAC_FLAGS)

BIN_DIR := bin/
SRC_DIR := application/src/main/java/
SOURCES := $(wildcard $(SRC_DIR)/*.java) $(wildcard $(SRC_DIR)/*/*.java) $(wildcard $(SRC_DIR)/*/*/*.java) $(wildcard $(SRC_DIR)/*/*/*/*.java)
CLASSES := $(patsubst $(SRC_DIR)/%.java,$(BIN_DIR)/%.class,$(SOURCES)) $(patsubst $(R_JAVA_DIR)/%.java,$(BIN_DIR)/%.class,$(R_JAVA_FILE))

DEX_FILE := $(patsubst %.apk,%.dex,$(TARGET))

UNSIGNED_TARGET := $(patsubst %.apk,%.unsigned.apk,$(TARGET))

KEYSTORE ?= debug.keystore
STOREPASS ?= android

all: $(TARGET)

.PHONY: resources install run clean aars libs

$(R_JAVA_FILE)_: resources
	mkdir -p "$$(dirname $@)"
	touch $@

$(R_JAVA_FILE) $(RES_APK_FILE): $(R_JAVA_FILE)_
	$(AAPT) package -f -m --auto-add-overlay -M $(MANIFEST) -S $(RES_DIR) -J $(R_JAVA_DIR) -I $(ANDROID_FRAMEWORK) -F $(RES_APK_FILE) --rename-manifest-package $(PACKAGE) --max-res-version $(ANDROID_TARGET_SDK)

ifndef HAVE_AARS
$(CLASSES): aars
	$(MAKE) HAVE_AARS=1 $@
else
$(CLASSES): $(SOURCES) $(R_JAVA_FILE)
	mkdir -p $(BIN_DIR)
	$(JAVAC) -cp $(CLASS_PATH) -sourcepath $(SRC_DIR) $(JAVAC_FLAGS) -d $(BIN_DIR) $+ $$(find $(R_JAVA_DIR) -type f -name '*.java')
endif

$(DEX_FILE): $(CLASSES)
	$(DEX) --dex --output $@ $(BIN_DIR) $(LINKED_JARS) $(patsubst %.aar,%.aar/res.apk,$(subst /,_,$(LIB_AARS)))

$(UNSIGNED_TARGET): $(RES_APK_FILE) $(DEX_FILE)
	$(APK_BUILDER) $@ -u -z $(RES_APK_FILE) -f $(DEX_FILE)

$(KEYSTORE):
	keytool -genkey -v -keystore $@ -storepass $(STOREPASS) -alias dbg -keypass $(STOREPASS) -keyalg RSA -keysize 2048 -dname "CN=Android Debug" -validity 10000

$(TARGET): $(UNSIGNED_TARGET) $(KEYSTORE)
	$(JAR_SIGNER) -keystore $(KEYSTORE) -storepass $(STOREPASS) -sigalg SHA1withRSA -digestalg SHA1 $(UNSIGNED_TARGET) dbg
	$(ZIP_ALIGN) -f 4 $(UNSIGNED_TARGET) $@

install:
	$(ADB) install -r $(TARGET)

run:
	$(ADB) shell am start $(PACKAGE)/.MainScreen

clean:
	rm -f $(R_JAVA_FILE) $(R_JAVA_FILE)_ $(RES_APK_FILE) $(DEX_FILE) $(UNSIGNED_TARGET) $(TARGET)
	rm -rf $(BIN_DIR)
	rm -rf $(subst /,_,$(LIB_AARS))

aars $(patsubst %.aar,%.aar/classes.jar,$(subst /,_,$(LIB_AARS))): $(LIB_JARS) $(LIB_AARS)
	for aar in $(LIB_AARS); do \
		dir="$$(echo "$$aar" | sed s:/:_:g)"; \
		mkdir -p "$$dir"; \
		unzip -ud "$$dir" "$$aar"; \
		mkdir -p $(R_JAVA_DIR); \
		$(AAPT) package -f -m --auto-add-overlay -M "$$dir"/AndroidManifest.xml -S "$$dir"/res -J $(R_JAVA_DIR) -I $(ANDROID_FRAMEWORK) -F "$$dir"/res.apk --max-res-version $(ANDROID_TARGET_SDK); \
	done
