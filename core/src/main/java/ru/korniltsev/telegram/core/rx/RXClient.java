package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.util.Log;
import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TG;
import org.drinkless.td.libcore.telegram.TdApi;
import org.drinkless.td.libcore.telegram.TdApi.TLObject;
import ru.korniltsev.telegram.core.adapters.RequestHandlerAdapter;
import rx.Observable;
import rx.subjects.PublishSubject;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Created by korniltsev on 21/04/15.
 */
public class RXClient {

    private Context ctx;

    private final Client client;
    private final PublishSubject<TdApi.TLObject> globalSubject = PublishSubject.create();

    public RXClient(Context ctx) {
        this.ctx = ctx;
        TG.setUpdatesHandler(globalSubject::onNext);
        TG.setDir(ctx.getFilesDir().getAbsolutePath() + "/");
        this.client = TG.getClientInstance();
//        globalSubject.subscribe(o -> {
//            Log.d("RxClient.Global", o.toString());
//        });
        filesUpdates()
                .subscribe(u -> Log.d("FileUpdates", String.valueOf(u.fileId)));
    }

    public Observable<TLObject> sendRXUI(final TdApi.TLFunction function) {
        return sendRXUI(function, 0);
    }

    //observe function on ui thread
    public Observable<TLObject> sendRXUI(final TdApi.TLFunction function, long delay) {
        return sendRX(function, delay)
                .cache()
                .observeOn(mainThread());
    }

    //observe function
    private Observable<TLObject> sendRX(final TdApi.TLFunction function, final long delay) {
        return Observable.create(s -> {
            client.send(function, object -> {
                //                SystemClock.sleep(delay);

                if (object instanceof TdApi.Error) {
                    Log.e("RxClient", ((TdApi.Error) object).text);
                    s.onError(new RxClientException((TdApi.Error) object));
                } else {
                    s.onNext(object);
                    s.onCompleted();
                }
            });
        });
    }

    public void sendSilently(final TdApi.TLFunction function) {
        client.send(function, RequestHandlerAdapter.INSTANCE);
    }

    public Observable<TdApi.User> getUser(int id) {
        return sendRXUI(new TdApi.GetUser(id)).map( o -> (TdApi.User) o);
    }

    public Observable<TdApi.GroupChatFull> getGroupChatInfo(int id) {
        return sendRXUI(new TdApi.GetGroupChatFull(id))
                .map(o -> (TdApi.GroupChatFull) o);
    }

    static class RxClientException extends Exception {
        public final TdApi.Error error;

        public RxClientException(TdApi.Error error) {
            super(error.text);
            this.error = error;
        }
    }

    public Observable<TdApi.UpdateFile> filesUpdates() {
        return globalSubject
                .filter(tlObject -> tlObject instanceof TdApi.UpdateFile)
                .map(o -> (TdApi.UpdateFile) o)
                .observeOn(mainThread());
    }

    /*public Observable<TdApi.UpdateFile> fileUpdate(final TdApi.FileEmpty file) {
        sendSilently(new TdApi.DownloadFile(file.id));
        return filesUpdates()
                .filter(updateFile -> updateFile.fileId == file.id);
    }*/

    public Client getClient() {
        return client;
    }

    ////////////

    public Observable<TdApi.Chats> getChats(int offset, int limit) {
        return sendRXUI(new TdApi.GetChats(offset, limit), 0)
                .map(o -> (TdApi.Chats) o);
    }

    public Observable<TdApi.User> getMe() {
        return sendRXUI(new TdApi.GetMe(), 0)
                .map(o -> (TdApi.User) o);
    }

    public Observable<TdApi.Messages> getMessages(final long chatId, final int fromId, final int offset, final int limit) {
        return sendRXUI(new TdApi.GetChatHistory(chatId, fromId, offset, limit), 0)
                .map(o -> (TdApi.Messages) o);
    }
}
