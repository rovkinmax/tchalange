package ru.korniltsev.telegram.core.picasso;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import webp.SupportBitmapFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertTrue;

public class TDFileRequestHandler extends RequestHandler {

    public static final String TD_FILE = "td.file";
    public static final String FILE_PATH = "file_path";
    public static final String WEBP = "webp";
    public static final String ID = "id";
    private static final long TIMEOUT = 25000;

    public static Uri load(TdApi.File f, boolean webp) {
        if (f instanceof TdApi.FileLocal) {
            TdApi.FileLocal local = (TdApi.FileLocal) f;
            return new Uri.Builder()
                    .scheme(TD_FILE)
                    .appendQueryParameter(FILE_PATH, local.path)
                    .appendQueryParameter(WEBP, String.valueOf(webp))
                    .build();
        } else {
            TdApi.FileEmpty e = (TdApi.FileEmpty) f;
            assertTrue(e.id != 0);
            return new Uri.Builder()
                    .scheme(TD_FILE)
                    .appendQueryParameter(ID, String.valueOf(e.id))
                    .appendQueryParameter(WEBP, String.valueOf(webp))
                    .build();
        }
    }

    final RxDownloadManager downloader;

    public TDFileRequestHandler(RxDownloadManager downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.uri.getScheme().equals(TD_FILE);
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        Uri uri = request.uri;
        boolean webp = Boolean.parseBoolean(uri.getQueryParameter(WEBP));
        String strId = uri.getQueryParameter(ID);
        String path;
        if (strId == null) {
            path = uri.getQueryParameter(FILE_PATH);
        } else {
            int id = Integer.parseInt(strId);
            path = downloadAndGetPath(id);//uri.getQueryParameter(FILE_PATH);
        }

        //        https://code.google.com/p/webp/issues/detail?id=147
        //        WebP support for transparent files was added in Android JB-MR2 (4.2) onwards.
        if (webp && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Bitmap bitmap = SupportBitmapFactory.decodeWebPBitmap(path);
            if (bitmap != null) {
                return new Result(
                        bitmap,
                        Picasso.LoadedFrom.NETWORK);
            } else {
                //may be it is not webp
                return new Result(new FileInputStream(path), Picasso.LoadedFrom.NETWORK);
            }
        } else {
            return new Result(new FileInputStream(path), Picasso.LoadedFrom.NETWORK);
        }
    }


    private String downloadAndGetPath(int id) throws IOException {
        try {
            TdApi.FileLocal first = downloader.download(id)
                    .compose(RxDownloadManager.ONLY_RESULT)
                    .first()
                    .toBlocking()
                    .toFuture()
                    .get(TIMEOUT, TimeUnit.MILLISECONDS);
            return first.path;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (Throwable e) {
            Log.e("EmptyFileDataFetcher", "err", e);
            throw new IOException(e);
        }
    }
}
