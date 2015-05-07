package webp;

import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SupportBitmapFactory {

    static {
        System.loadLibrary("webpsupport");
    }

    private static native Bitmap nativeDecodeBitmap(ByteBuffer buffer, int len);

    public static Bitmap decodeWebPBitmap(String filePath) {
        RandomAccessFile r = null;
        try {
            File f = new File(filePath);
            r = new RandomAccessFile(filePath, "r");
            MappedByteBuffer map = r.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
            return nativeDecodeBitmap(map, map.limit());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (r != null){
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
