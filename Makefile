PACKAGE ?= com.mniip.bananapeel
TARGET := bananapeel.apk

AAPT ?= aapt
ADB ?= adb
JAVAC ?= javac
DEX ?= dx
APK_BUILDER ?= java -cp $(SDK_PATH)/tools/lib/sdklib.jar com.android.sdklib.build.ApkBuilderMain
JAR_SIGNER ?= jarsigner
ZIP_ALIGN ?= zipalign
ZIP ?= zip

PACKAGE_PATH := $(subst .,/,$(PACKAGE))

ANDROID_TARGET ?= android-16
ANDROID_FRAMEWORK := $(SDK_PATH)/platforms/$(ANDROID_TARGET)/android.jar

MANIFEST := AndroidManifest.xml
RES_DIR := res/
RES_APK_FILE := $(patsubst %.apk,%-res.apk,$(TARGET))

R_JAVA_DIR := gen/
R_JAVA_FILE := $(R_JAVA_DIR)/$(PACKAGE_PATH)/R.java

CLASS_PATH := $(ANDROID_FRAMEWORK):$(SDK_PATH)/extras/android/m2repository/com/android/support/support-v4/19.1.0/support-v4-19.1.0.jar
JAVAC_FLAGS := -target 1.7 -source 1.7 $(MY_JAVAC_FLAGS)

BIN_DIR := bin/
SRC_DIR := src/
SOURCES := $(wildcard $(SRC_DIR)/*.java) $(wildcard $(SRC_DIR)/*/*.java) $(wildcard $(SRC_DIR)/*/*/*.java) $(wildcard $(SRC_DIR)/*/*/*/*.java)
CLASSES := $(patsubst $(SRC_DIR)/%.java,$(BIN_DIR)/%.class,$(SOURCES)) $(patsubst $(R_JAVA_DIR)/%.java,$(BIN_DIR)/%.class,$(R_JAVA_FILE))

LIBS := $(SDK_PATH)/extras/android/m2repository/com/android/support/support-v4/19.1.0/support-v4-19.1.0.jar
DEX_FILE := $(patsubst %.apk,%.dex,$(TARGET))

UNSIGNED_TARGET := $(patsubst %.apk,%.unsigned.apk,$(TARGET))

KEYSTORE ?= debug.keystore
STOREPASS ?= android

all: $(TARGET)

.PHONY: resources install run clean

$(R_JAVA_FILE)_: resources
	touch $@

$(R_JAVA_FILE) $(RES_APK_FILE): $(R_JAVA_FILE)_
	mkdir -p $(R_JAVA_DIR)
	$(AAPT) package -f -m --auto-add-overlay -M $(MANIFEST) -S $(RES_DIR) -J $(R_JAVA_DIR) -I $(ANDROID_FRAMEWORK) -F $(RES_APK_FILE) --rename-manifest-package $(PACKAGE)

$(CLASSES): $(SOURCES) $(R_JAVA_FILE)
	mkdir -p $(BIN_DIR)
	$(JAVAC) -cp $(CLASS_PATH) $(JAVAC_FLAGS) -d $(BIN_DIR) $+

$(DEX_FILE): $(CLASSES)
	$(DEX) --dex --output $@ $(BIN_DIR) $(LIBS)

$(UNSIGNED_TARGET): $(RES_APK_FILE) $(DEX_FILE)
	$(APK_BUILDER) $@ -u -z $(RES_APK_FILE) -f $(DEX_FILE)

$(KEYSTORE):
	keytool -genkey -v -keystore $@ -storepass $(STOREPASS) -alias debug -keypass $(STOREPASS) -keyalg RSA -keysize 2048 -dname "CN=Android Debug" -validity 10000

$(TARGET): $(UNSIGNED_TARGET) $(KEYSTORE)
	$(JAR_SIGNER) -keystore $(KEYSTORE) -storepass $(STOREPASS) -sigalg SHA1withRSA -digestalg SHA1 $(UNSIGNED_TARGET) debug
	$(ZIP_ALIGN) -f 4 $(UNSIGNED_TARGET) $@

install:
	$(ADB) install -r $(TARGET)

run:
	$(ADB) shell am start $(PACKAGE)/.MainScreen

clean:
	rm -f $(R_JAVA_FILE) $(R_JAVA_FILE)_ $(RES_APK_FILE) $(DEX_FILE) $(UNSIGNED_TARGET) $(TARGET)
	rm -rf $(BIN_DIR)/*
