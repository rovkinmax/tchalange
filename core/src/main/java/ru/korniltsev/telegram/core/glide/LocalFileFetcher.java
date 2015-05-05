package ru.korniltsev.telegram.core.glide;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxGlide;

import java.io.FileInputStream;
import java.io.InputStream;

public class LocalFileFetcher implements DataFetcher<InputStream> {
    final TdApi.FileLocal file;
    private final String id;

    public LocalFileFetcher(TdApi.FileLocal file) {
        this.file = file;
        id = RxGlide.id(file);
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        return new FileInputStream(file.path);
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
