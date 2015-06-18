package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@Singleton
public class RxDownloadManager {
    public static final OnlyResult ONLY_RESULT = new OnlyResult();
    private Context ctx;
    final RXClient client;

    //guarded by lock
    private final Map<Integer, BehaviorSubject<FileState>> allRequests = new HashMap<>();
    //guarded by lock
    private Map<Integer, TdApi.FileLocal> allDownloadedFiles = new HashMap<>();

    final Object lock = new Object();

    //keep in memory names of files which we have copied to the external storage
    final Set<String> exposedFiles = new HashSet<>();

    @Inject
    public RxDownloadManager(Context ctx, RXClient client, RXAuthState auth) {
        this.ctx = ctx;
        this.client = client;
//        client
        client.filesUpdates()
                .subscribe(new ObserverAdapter<TdApi.UpdateFile>() {
                    @Override
                    public void onNext(TdApi.UpdateFile updateFile) {
                        updateFile(updateFile);
                    }
                });

        client.fileProgress().subscribe(new ObserverAdapter<TdApi.UpdateFileProgress>() {
            @Override
            public void onNext(TdApi.UpdateFileProgress upd) {
                updateFileProgress(upd);
            }
        });
        auth.listen().subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
            @Override
            public void onNext(RXAuthState.AuthState authState) {
                if (authState instanceof RXAuthState.StateLogout) {
                    cleanup();
                }
            }
        });
    }

    private void cleanup() {
        synchronized (lock) {
            allRequests.clear();
            allDownloadedFiles.clear();
            exposedFiles.clear();
        }
    }

    private void updateFileProgress(TdApi.UpdateFileProgress upd) {
        synchronized (lock){
            BehaviorSubject<FileState> s = allRequests.get(upd.fileId);
            s.onNext(new FileProgress(upd));
        }
    }

    private void updateFile(TdApi.UpdateFile upd) {
//        log("before handleUpdateFile:" + RXClient.coolTagForFileId(upd.fileId));
        synchronized (lock) {
//            log("handleUpdateFile:" + RXClient.coolTagForFileId(upd.fileId));
            TdApi.FileLocal f = new TdApi.FileLocal(upd.fileId, upd.size, upd.path);
            allDownloadedFiles.put(upd.fileId, f);
            BehaviorSubject<FileState> s = allRequests.get(upd.fileId);
            if (s != null){
                s.onNext(new FileDownloaded(f));
            }
        }
    }

    private void log(String msg) {
        Log.e("RxDownloadManager", msg);
    }


    public Observable<FileState> download(final TdApi.File f){
        if (f instanceof TdApi.FileEmpty) {
            return download(((TdApi.FileEmpty) f));
        } else {
            TdApi.FileLocal f1 = (TdApi.FileLocal) f;
            return Observable.<FileState>just(new FileDownloaded(f1));
        }
    }

    public Observable<TdApi.FileLocal> downloadWithoutProgress(TdApi.File f) {
        return download(f)
                .compose(ONLY_RESULT);

    }



    public Observable<FileState> download(final TdApi.FileEmpty file) {
        int id = file.id;
        return download(id);
    }

    /**
     *
     * @param id of EmptyFile
     * @return
     */
    @NonNull public Observable<FileState> download(int id) {
        synchronized (lock) {
//            log("download " + RXClient.coolTagForFileId(id));
            assertTrue(id != 0);
            //            assertFalse(isDownloading(file));
            BehaviorSubject<FileState> prevRequest = allRequests.get(id);
            if (prevRequest != null) {
//                log("return prev request" + RXClient.coolTagForFileId(id));
                return prevRequest;
            }
            TdApi.FileLocal fileLocal = allDownloadedFiles.get(id);
            if (fileLocal != null) {
//                log("already downloaded " + RXClient.coolTagForFileId(id));
                //                BehaviorSubject<FileState> newRequest = BehaviorSubject.create(fileLocal);
                //                allRequests.put(id, newRequest);
                return Observable.<FileState>just(new FileDownloaded(fileLocal));//newRequest;
            }
//            log("create new request" + RXClient.coolTagForFileId(id));
            final BehaviorSubject<FileState> s = BehaviorSubject.create();
            allRequests.put(id, s);
            client.sendSilently(new TdApi.DownloadFile(id));
            return s;
        }
    }

    public boolean isDownloaded(TdApi.File file) {
        if (file instanceof TdApi.FileLocal) {
            return true;
        }
        TdApi.FileEmpty e = (TdApi.FileEmpty) file;
        return getDownloadedFile(e.id) != null;
    }



    public boolean isDownloading(TdApi.FileEmpty file) {
        return nonMainThreadObservableFor(file) != null;
    }

    @Nullable
    public Observable<FileState> nonMainThreadObservableFor(TdApi.FileEmpty file) {
        synchronized (lock) {
            return allRequests.get(file.id);
        }
    }

    @Nullable
    public TdApi.FileLocal getDownloadedFile(Integer id) {
        synchronized (lock) {
            return allDownloadedFiles.get(id);
        }
    }

    public TdApi.FileLocal getDownloadedFile(TdApi.File f){
        if (f instanceof TdApi.FileLocal){
            return (TdApi.FileLocal) f;
        } else {
            TdApi.FileEmpty e = (TdApi.FileEmpty) f;
            return getDownloadedFile(e.id);
        }
    }

    public File exposeFile(File src, String type, @Nullable String originalFileName) {
        File dstDir = ctx.getExternalFilesDir(type);
        String name = src.getName();
        File dst = new File(dstDir, originalFileName == null? name: originalFileName);
        if (exposedFiles.contains(name)) {
        } else {
            try {
                Utils.copyFile(src, dst);
            } catch (IOException e) {
                return dst;
            }
        }
        return dst;

    }

    public class FileState {

    }

    public class FileDownloaded extends FileState {
        public final TdApi.FileLocal f;

        public FileDownloaded(TdApi.FileLocal f) {
            this.f = f;
        }
    }

    public class FileProgress extends FileState{
        public final TdApi.UpdateFileProgress p;

        public FileProgress(TdApi.UpdateFileProgress p) {
            this.p = p;
        }
    }

    public static class OnlyResult implements Observable.Transformer<FileState, TdApi.FileLocal> {
        @Override
        public Observable<TdApi.FileLocal> call(Observable<FileState> fileStateObservable) {
            return fileStateObservable.filter(new Func1<FileState, Boolean>() {
                @Override
                public Boolean call(FileState fileState) {
                    return fileState instanceof FileDownloaded;
                }
            }).map(new Func1<FileState, TdApi.FileLocal>() {
                @Override
                public TdApi.FileLocal call(FileState fileState) {
                    return ((FileDownloaded) fileState).f;
                }
            });
        }
    }
}
