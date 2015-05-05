package ru.korniltsev.telegram.core.glide;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxGlide;
import rx.Observable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class EmptyFileDataFetcher implements DataFetcher<InputStream> {

    public static final int TIMEOUT = 20000;

    private final TdApi.FileEmpty model;
    private final String id;
    private final RXClient client;

    public EmptyFileDataFetcher(TdApi.FileEmpty model, RXClient client) {
        this.model = model;
        this.client = client;
        this.id = RxGlide.id(model);
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        Observable<TdApi.UpdateFile> specificFileUpdate = client.fileUpdate(model)
                .first()
                .cache();

        //todo single download entry point - move tod donwload manager
        client.sendSilently(new TdApi.DownloadFile(model.id));
        TdApi.UpdateFile first = specificFileUpdate.toBlocking()
                .toFuture()
                .get(TIMEOUT, TimeUnit.MILLISECONDS);

        return new FileInputStream(new File(first.path));
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
