package ru.korniltsev.telegram.core.glide;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.module.GlideModule;
import dagger.ObjectGraph;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;

import java.io.InputStream;

public class TDGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDiskCache(new DiskCache.Factory() {
            @Override
            public DiskCache build() {
                return null;
            }
        });
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        ObjectGraph graph = ObjectGraphService.getObjectGraph(context);
        final RXClient client = graph.get(RXClient.class);

        glide.register(TdApi.File.class,
                InputStream.class,
                new ModelLoaderFactory<TdApi.File, InputStream>() {
                    @Override
                    public ModelLoader<TdApi.File, InputStream> build(Context context, GenericLoaderFactory factories) {
                        return new FileModelLoader(client);
                    }

                    @Override
                    public void teardown() {

                    }
                });
        glide.register(Stub.class,
                InputStream.class, new ModelLoaderFactory<Stub, InputStream>() {
                    @Override
                    public ModelLoader<Stub, InputStream> build(Context context, GenericLoaderFactory factories) {
                        return new StubModelLoader(context);
                    }

                    @Override
                    public void teardown() {

                    }
                });
    }
}
