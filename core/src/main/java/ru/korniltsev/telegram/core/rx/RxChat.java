package ru.korniltsev.telegram.core.rx;

import android.util.SparseArray;
import android.util.SparseIntArray;
import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.addAll;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class RxChat implements UserHolder {
    final long id;
    final RXClient client;
    final RxChatDB holder;

    private final List<TdApi.Message> messages = new ArrayList<>();
    private final PublishSubject<List<TdApi.Message>> subject = PublishSubject.create();
    private Observable<RxChatDB.Portion> request;
    private Set<Integer> tmpUIDs = new HashSet<>();

    private boolean atLeastOneRequestCompleted = false;
    private boolean downloadedAll;

    public RxChat(long id, RXClient client, RxChatDB holder) {
        this.id = id;
        this.client = client;
        this.holder = holder;
    }

    public boolean atLeastOneRequestCompleted() {
        return atLeastOneRequestCompleted;
    }

    public boolean isRequestInProgress() {
        return request != null;
    }

    public void request2(TdApi.Message lastMessage, final TdApi.Message initMessage) {
        requestImpl(lastMessage, initMessage, true, RxChatDB.LIMIT, 0);
    }

    private void requestImpl(TdApi.Message lastMessage, final TdApi.Message initMessage, final boolean historyRequest, int limit, int offset) {
        assertNull(request);
        //        Log.e("RxChat", "request", new Throwable());
        request = client.getMessages(id, lastMessage.id, offset, limit)
                .flatMap(new Func1<TdApi.Messages, Observable<? extends RxChatDB.Portion>>() {
                    @Override
                    public Observable<? extends RxChatDB.Portion> call(TdApi.Messages portion) {
                        checkNotMainThread();
                        TdApi.Message[] messages = portion.messages;
                        tmpUIDs.clear();//todo boxing
                        for (TdApi.Message message : messages) {
                            getUIDs(message);
                        }
                        if (initMessage != null) {
                            getUIDs(initMessage);
                        }

                        final List<TdApi.Message> messageList = new ArrayList<>();
                        if (initMessage != null) {
                            messageList.add(initMessage);
                        }
                        addAll(messageList, portion.messages);
                        for (TdApi.Message message : messageList) {
                            holder.parser.parse(message);
                        }

                        if (tmpUIDs.isEmpty()) {
                            RxChatDB.Portion res = new RxChatDB.Portion(messageList, Collections.<TdApi.User>emptyList());
                            return Observable.just(res);
                        } else {
                            List<Observable<TdApi.User>> os = new ArrayList<>();
                            for (Integer uid : tmpUIDs) {
                                os.add(client.getUser(uid));
                            }
                            //request missing users and zip
                            Observable<List<TdApi.User>> allUsers = Observable.merge(os)
                                    .toList();

                            Observable<List<TdApi.Message>> messagesCopy = Observable.just(messageList);
                            return allUsers.zipWith(messagesCopy, RxChatDB.ZIPPER);
                        }
                    }
                })
                .observeOn(mainThread());

        request.subscribe(new Action1<RxChatDB.Portion>() {
            @Override
            public void call(RxChatDB.Portion portion) {
                checkMainThread();
                request = null;
                atLeastOneRequestCompleted = true;
                if (portion.ms.isEmpty()) {
                    downloadedAll = true;
                } else {


                    for(int i = 0; i < portion.us.size(); i++) {
                        TdApi.User obj = portion.us.get(
                                portion.us.keyAt(i));
                        holder.saveUser(obj);
                    }
                    if (!historyRequest) {
                        messages.clear();
                    }
                    messages.addAll(portion.ms);
                    subject.onNext(messages);
                }
            }
        });
    }

    private void getUIDs(TdApi.Message message) {
        assertTrue(message.fromId != 0);
        if (!hasUserWith(message.fromId)) {
            tmpUIDs.add(message.fromId);
        }
        if (message.forwardFromId != 0) {
            if (!hasUserWith(message.forwardFromId)) {
                tmpUIDs.add(message.forwardFromId);
            }
        }
    }

    public Observable<List<TdApi.Message>> messageList() {
        return subject;
    }

    public List<TdApi.Message> getMessages() {
        return messages;
    }

    public boolean isDownloadedAll() {
        return downloadedAll;
    }

    @Override
    public boolean hasUserWith(int id) {
        return holder.hasUserWith(id);
    }

    @Override
    public TdApi.User getUser(int id) {
        return holder.getUser(id);
    }

    @Override
    public void saveUser(TdApi.User u) {
        holder.saveUser(u);
    }

    void updateCurrentMessageList(boolean newMessage) {
        checkMainThread();
        if (messages.isEmpty()) {
            return;
        }
        if (request == null) {
            int offset = -1;
            int size = messages.size();
            if (newMessage) {
                offset--;
                size++;
                size++;
            }
            requestImpl(messages.get(0), null, false, size, offset);
        } else {

        }
    }

    public void sendMessage(String text) {
        TdApi.InputMessageText content = new TdApi.InputMessageText(text);
        client.sendRx(new TdApi.SendMessage(id, content))
                .map(new Func1<TdApi.TLObject, TdApi.Message>() {
                    @Override
                    public TdApi.Message call(TdApi.TLObject tlObject) {
                        return (TdApi.Message) tlObject;
                    }
                })
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject tlObject) {
                        updateCurrentMessageList(true);
                    }
                });
    }

    public void deleteHistory() {
        client.sendRx(new TdApi.DeleteChatHistory(id))
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject o) {
                        messages.clear();
                        subject.onNext(messages);
                    }
                });
    }

    final SparseArray<TdApi.UpdateMessageId> newIdToUpdate = new SparseArray<>();
    public void updateMessageId(TdApi.UpdateMessageId upd) {
        newIdToUpdate.put(upd.newId, upd);
    }

    public TdApi.UpdateMessageId get(int newId) {
        return newIdToUpdate.get(newId);
    }
}
