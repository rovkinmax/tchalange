package ru.korniltsev.telegram.core.glide;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class StubModelLoader implements ModelLoader<Stub, Bitmap> {
    final Context ctx;

    public StubModelLoader(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public DataFetcher<Bitmap> getResourceFetcher(Stub model, int width, int height) {
        return new UserStubFetcher(model, height, width, ctx);
    }
}
