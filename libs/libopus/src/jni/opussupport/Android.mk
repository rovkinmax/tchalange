LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := opussupport2

LOCAL_SRC_FILES := \
	jni_load.cpp  \
	opus_OpusSupport.cpp



LOCAL_C_INCLUDES    := \
	$(LOCAL_PATH)  \
	$(LOCAL_PATH)/../opus/include \
	$(LOCAL_PATH)/../opus/silk \
	$(LOCAL_PATH)/../opus/silk/fixed \
	$(LOCAL_PATH)/../opus/celt \
	$(LOCAL_PATH)/../opus/ \
	$(LOCAL_PATH)/../opus/opusfile \


#LOCAL_C_INCLUDES := \
#	$(LOCAL_PATH) \
#	$(LOCAL_PATH)/../opus/include \
#	$(LOCAL_PATH)/../opus/

LOCAL_STATIC_LIBRARIES := opus2

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
