package opus;

import java.nio.ByteBuffer;

public class OpusSupport {
    static {
        System.loadLibrary("opussupport2");
    }
    public static native boolean nativeIsOpusFile(String path);
    public static native boolean nativeOpenOpusFile(String path);
    public static native void nativeCloseOpusFile();
    public static native long nativeGetTotalPcmDuration();
    public static native void nativeReadOpusFile(ByteBuffer buffer, int capacity, int[] args);
    public static native boolean nativeSeekOpusFile(float position);
}
