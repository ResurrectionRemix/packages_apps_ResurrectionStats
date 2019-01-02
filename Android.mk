LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PRIVATE_PLATFORM_APIS := false
LOCAL_PACKAGE_NAME := ResurrectionStats
LOCAL_CERTIFICATE := platform
LOCAL_STATIC_JAVA_LIBRARIES := androidx.core_core androidx.appcompat_appcompat

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
