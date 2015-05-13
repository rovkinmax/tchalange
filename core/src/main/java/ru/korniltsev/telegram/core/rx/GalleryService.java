package ru.korniltsev.telegram.core.rx;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.core.utils.Preconditions;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;

import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

@Singleton
public class GalleryService {
    final Context ctx;
    final RxDownloadManager downloader;
    private final SharedPreferences prefs;
    private final String appName;

    @Inject
    public GalleryService(Context ctx, RxDownloadManager downloader) {
        this.ctx = ctx;
        appName = ctx.getApplicationInfo().loadLabel(ctx.getPackageManager()).toString();
        this.downloader = downloader;
        prefs = ctx.getSharedPreferences("GalleryService", Context.MODE_PRIVATE);
    }

    private int increaseCounter() {
        int counter = prefs.getInt("counter", 0);
        prefs.edit()
                .putInt("counter", counter + 1)
                .commit();
        return counter;
    }

    public Observable<File> saveToGallery(final TdApi.Photo photo) {
        return saveToGallerySync(photo);
    }

    private Observable<File> saveToGallerySync(TdApi.Photo photo) {
        TdApi.PhotoSize biggestSize = PhotoUtils.findBiggestSize(photo);
        return downloader.download(biggestSize.photo)
                .observeOn(io())
                .flatMap(new Func1<TdApi.FileLocal, Observable<File>>() {
                    @Override
                    public Observable<File> call(TdApi.FileLocal fileLocal) {
                        checkNotMainThread();

                        return Observable.just(
                                copyFileToGallery(fileLocal));
                    }
                })
                .observeOn(mainThread())
                .map(new Func1<File, File>() {
                    @Override
                    public File call(File f) {
                        checkMainThread();
                        scanFile(f);
                        return f;
                    }
                });
    }

    private void scanFile(File f) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        ctx.sendBroadcast(mediaScanIntent);
    }

    private File copyFileToGallery(TdApi.FileLocal fileLocal) {
        File picturesDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                appName);
        picturesDir.mkdirs();
        File dst = new File(picturesDir, increaseCounter() + ".jpeg");//todo may be not jpeg
        File src = new File(fileLocal.path);
        try {
            Utils.copyFile(src, dst);
            return dst;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
