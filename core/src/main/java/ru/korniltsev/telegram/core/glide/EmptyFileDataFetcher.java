package ru.korniltsev.telegram.core.glide;

import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.glide.stub.FilePath;
import ru.korniltsev.telegram.core.glide.stub.FileReference;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.RxGlide;

import java.util.concurrent.TimeUnit;

public class EmptyFileDataFetcher implements DataFetcher<FilePath> {

    public static final int TIMEOUT = 5000;

    private final FileReference model;
    private final String id;
    private final RxDownloadManager downloader;
    private final TdApi.FileEmpty file;

    public EmptyFileDataFetcher(FileReference model, RxDownloadManager downloader) {
        this.model = model;
        this.downloader = downloader;
        file = (TdApi.FileEmpty) model.file;
        this.id = RxGlide.id(file);
    }

    @Override
    public FilePath loadData(Priority priority) throws Exception {
        try {
            TdApi.FileLocal first = downloader.download(file)
                    .first()
                    .toBlocking()
                    .toFuture()
                    .get(TIMEOUT, TimeUnit.MILLISECONDS);
            return new FilePath(first.path, model.webp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Throwable e) {
            Log.e("EmptyFileDataFetcher", "err", e);
            throw e;
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void cancel() {

    }
}
