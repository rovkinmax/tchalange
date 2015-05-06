package ru.korniltsev.telegram.core.glide;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;

import java.io.InputStream;

//can load any file except if file.id == 0 in this case it fails
public class FileModelLoader implements ModelLoader<TdApi.File, InputStream> {
    final RxDownloadManager downloader;

    public FileModelLoader(RxDownloadManager downloader) {
        this.downloader = downloader;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(TdApi.File model, int width, int height) {
        if (model instanceof TdApi.FileLocal) {
            TdApi.FileLocal photoSmall = (TdApi.FileLocal) model;
            return new LocalFileFetcher(photoSmall);
        } else {
            TdApi.FileEmpty photoSmall = (TdApi.FileEmpty) model;
            if (photoSmall.id == 0) {
                Assert.fail();
                return null;
            } else {
                return new EmptyFileDataFetcher(photoSmall, downloader);
            }
        }
    }
}
