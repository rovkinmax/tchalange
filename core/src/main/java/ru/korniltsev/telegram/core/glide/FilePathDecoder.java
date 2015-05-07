package ru.korniltsev.telegram.core.glide;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableResource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import ru.korniltsev.telegram.core.glide.stub.FilePath;
import webp.SupportBitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FilePathDecoder implements ResourceDecoder<FilePath, Bitmap> {
    private final Downsampler downsampler;
    private final BitmapPool bitmapPool;
    private final DecodeFormat decodeFormat;
    private final Resources res;

    public FilePathDecoder(Context context) {
        this.downsampler = Downsampler.AT_LEAST;
        this.bitmapPool = Glide.get(context).getBitmapPool();
        this.decodeFormat = DecodeFormat.PREFER_ARGB_8888;
        res = context.getResources();
    }

    @Override
    public Resource<Bitmap> decode(FilePath source, int width, int height) throws IOException {
        if (source.webp) {
            return decodeSupport(source);
        }
        Bitmap bitmap = downsampler.decode(new FileInputStream(source.path), bitmapPool, width, height, decodeFormat);
        return BitmapResource.obtain(bitmap, bitmapPool);//new BitmapDrawableResource(new Bitmap(res, bitmap), bitmapPool);

    }

    private Resource<Bitmap> decodeSupport(FilePath source) throws IOException {
//        FileInputStream fis = new FileInputStream(source.path);
//        RandomAccessFile file = null;
//        try {
//            File f = new File(source.path);
//            file = new RandomAccessFile(f, "r");
//            ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());

            Bitmap bitmap = SupportBitmapFactory.nativeDecodeBitmap(source.path);//WebPFactory.loadWebpImage(buffer, buffer.limit(), null);
            return BitmapResource.obtain(bitmap, bitmapPool);
//        } finally {
//            if (file != null){
//                file.close();
//            }
//        }


    }

    @Override
    public String getId() {
        return "telegram decode bitmap";
    }
}
