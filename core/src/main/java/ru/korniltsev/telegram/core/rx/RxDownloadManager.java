package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class RxDownloadManager {
    final RXClient client;

    //guarded by ui thread
    final Map<Integer, BehaviorSubject<TdApi.UpdateFile>> allRequests = new HashMap<>();

    @Inject
    public RxDownloadManager(RXClient client) {
        this.client = client;
    }

    public void download(final TdApi.FileEmpty file) {
        assertTrue(file.id != 0);
        assertFalse(isDownloading(file));

        final BehaviorSubject<TdApi.UpdateFile> s = BehaviorSubject.create();
        client.filesUpdates()
                .filter(new Func1<TdApi.UpdateFile, Boolean>() {
                    @Override
                    public Boolean call(TdApi.UpdateFile updateFile) {
                        return updateFile.fileId == file.id;
                    }
                }).first()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateFile>() {
                    @Override
                    public void call(TdApi.UpdateFile updateFile) {
                        s.onNext(updateFile);
                    }
                });
        allRequests.put(file.id, s);
        client.sendSilently(new TdApi.DownloadFile(file.id));
    }

    public boolean isDownloaded(TdApi.File file) {
        if (file instanceof TdApi.FileLocal) {
            return true;
        }
        TdApi.FileEmpty e = (TdApi.FileEmpty) file;
        return client.getDownloadedFile(e.id) != null;
    }

    public boolean isDownloading(TdApi.FileEmpty file) {
        return observableFor(file) != null;
    }

    public Observable<TdApi.UpdateFile> observableFor(TdApi.FileEmpty file) {
        return allRequests.get(file.id);
    }
}
