package ru.korniltsev.telegram.core.glide.stub;

import org.drinkless.td.libcore.telegram.TdApi;

public class FileReference  {
    public final TdApi.File file;
    public final boolean webp;

    public FileReference(TdApi.File file, boolean webp) {
        this.file = file;
        this.webp = webp;
    }
}
