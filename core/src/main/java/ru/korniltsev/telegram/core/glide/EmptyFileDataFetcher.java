package ru.korniltsev.telegram.core.glide;

import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.RxGlide;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EmptyFileDataFetcher implements DataFetcher<InputStream> {

    public static final int TIMEOUT = 5000;

    private final TdApi.FileEmpty model;
    private final String id;
    private final RxDownloadManager downloader;

    public EmptyFileDataFetcher(TdApi.FileEmpty model, RxDownloadManager downloader) {
        this.model = model;
        this.downloader = downloader;
        this.id = RxGlide.id(model);
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        try {
            Log.d("EmptyFileDataFetcher", "load" + model.id);
            TdApi.FileLocal first = downloader.download(model)
                    .first()
                    .toBlocking()
                    .toFuture()
                    .get(TIMEOUT, TimeUnit.MILLISECONDS);
            Log.d("EmptyFileDataFetcher", "load complete " + model.id);
            return new FileInputStream(new File(first.path));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("EmptyFileDataFetcher", "err", e);
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
