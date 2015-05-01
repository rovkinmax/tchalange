package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.util.Log;
import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TG;
import org.drinkless.td.libcore.telegram.TdApi;
import org.drinkless.td.libcore.telegram.TdApi.TLObject;
import ru.korniltsev.telegram.core.adapters.RequestHandlerAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
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
        TG.setUpdatesHandler(new Client.ResultHandler() {
            @Override
            public void onResult(TLObject object) {
                globalSubject.onNext(object);
            }
        });
        TG.setDir(ctx.getFilesDir().getAbsolutePath() + "/");
        this.client = TG.getClientInstance();

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
        return Observable.create(new Observable.OnSubscribe<TLObject>() {
            @Override
            public void call(final Subscriber<? super TLObject> s) {
                client.send(function, new Client.ResultHandler() {
                    @Override
                    public void onResult(TLObject object) {
                        if (object instanceof TdApi.Error) {
                            Log.e("RxClient", ((TdApi.Error) object).text);
                            s.onError(new RxClientException((TdApi.Error) object));
                        } else {
                            s.onNext(object);
                            s.onCompleted();
                        }
                    }
                });
            }
        });
    }

    public void sendSilently(final TdApi.TLFunction function) {
        client.send(function, RequestHandlerAdapter.INSTANCE);
    }

    public Observable<TdApi.User> getUser(int id) {
        return sendRXUI(new TdApi.GetUser(id))
                .map(new Func1<TLObject, TdApi.User>() {
                    @Override
                    public TdApi.User call(TLObject o) {
                        return (TdApi.User) o;
                    }
                });
    }

    public Observable<TdApi.GroupChatFull> getGroupChatInfo(int id) {
        return sendRXUI(new TdApi.GetGroupChatFull(id))
                .map(new Func1<TLObject, TdApi.GroupChatFull>() {
                    @Override
                    public TdApi.GroupChatFull call(TLObject o) {
                        return (TdApi.GroupChatFull) o;
                    }
                });
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
                .filter(new Func1<TLObject, Boolean>() {
                    @Override
                    public Boolean call(TLObject tlObject) {
                        return tlObject instanceof TdApi.UpdateFile;
                    }
                })
                .map(new Func1<TLObject, TdApi.UpdateFile>() {
                    @Override
                    public TdApi.UpdateFile call(TLObject o) {
                        return (TdApi.UpdateFile) o;
                    }
                })
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
                .map(new Func1<TLObject, TdApi.Chats>() {
                    @Override
                    public TdApi.Chats call(TLObject o) {
                        return (TdApi.Chats) o;
                    }
                });
    }

    public Observable<TdApi.User> getMe() {
        return sendRXUI(new TdApi.GetMe(), 0)
                .map(new Func1<TLObject, TdApi.User>() {
                    @Override
                    public TdApi.User call(TLObject o) {
                        return (TdApi.User) o;
                    }
                });
    }

    public Observable<TdApi.Messages> getMessages(final long chatId, final int fromId, final int offset, final int limit) {
        return sendRXUI(new TdApi.GetChatHistory(chatId, fromId, offset, limit), 0)
                .map(new Func1<TLObject, TdApi.Messages>() {
                    @Override
                    public TdApi.Messages call(TLObject o) {
                        return (TdApi.Messages) o;
                    }
                });
    }
}
