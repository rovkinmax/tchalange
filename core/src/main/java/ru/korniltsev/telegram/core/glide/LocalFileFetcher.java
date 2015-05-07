package ru.korniltsev.telegram.core.glide;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.glide.stub.FilePath;
import ru.korniltsev.telegram.core.glide.stub.FileReference;
import ru.korniltsev.telegram.core.rx.RxGlide;

public class LocalFileFetcher implements DataFetcher<FilePath> {
//    final TdApi.FileLocal file;
    private final String id;
    private final FileReference model;
    private final TdApi.FileLocal file;

    public LocalFileFetcher(FileReference file) {
        this.model = file;
        this.file = (TdApi.FileLocal) file.file;
        id = RxGlide.id(this.file);
    }

    @Override
    public FilePath loadData(Priority priority) throws Exception {
        return new FilePath(file.path, model.webp);//FileInputStream(file.path);
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
