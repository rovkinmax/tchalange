package ru.korniltsev.telegram.chat;

import android.support.annotation.Nullable;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Adapter.Portion;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.addAll;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

//todo delete it i dont like it
public class MessagesHolder {

    public static final Func2<List<TdApi.User>, List<TdApi.Message>, Portion> ZIPPER = new Func2<List<TdApi.User>, List<TdApi.Message>, Portion>() {
        @Override
        public Portion call(List<TdApi.User> users, List<TdApi.Message> messages) {
            return new Portion(messages, users);
        }
    };


    final RXClient client;
    final TdApi.Chat chat;
    final AddListener delegate;


    //GuardedBy Tdlib Client thread
    private final Set<Integer> users = new HashSet<>();


    private Observable<Portion> request;
    private boolean downloadedAll;
    private boolean atLeastOneRequestCompleted;

    public MessagesHolder(RXClient client, TdApi.Chat chat, AddListener delegate) {
        this.client = client;
        this.chat = chat;
        this.delegate = delegate;
    }




    public boolean isRequestInProgress() {
        return request != null;
    }

    public Subscription subscribe() {
        return request.subscribe(new Action1<Portion>() {
            @Override
            public void call(Portion portion) {
                request = null;

                if (portion.ms.isEmpty()) {
                    downloadedAll = true;
                } else {
//                    add(portion, true);
                    delegate.historyAdded(portion);
                }
            }
        });
    }

    public Subscription subscribeNewMessages() {
        return client.newMessageUpdate(chat.id)
                .flatMap(new Func1<TdApi.UpdateNewMessage, Observable<Portion>>() {
                    @Override
                    public Observable<Portion> call(TdApi.UpdateNewMessage upd) {
                        users.clear();
                        getUIDs(upd.message);
                        List<TdApi.Message> updSingleton = Collections.singletonList(upd.message);
                        if (users.isEmpty()) {
                            Portion res = new Portion(updSingleton, Collections.<TdApi.User>emptyList());
                            return Observable.just(res);
                        } else {

                            List<Observable<TdApi.User>> os = new ArrayList<>();
                            for (Integer uid : users) {
                                os.add(client.getUser(uid));
                            }
                            //request missing users and zip
                            Observable<List<TdApi.User>> allUsers = Observable.merge(os)
                                    .toList();

                            Observable<List<TdApi.Message>> just = Observable.just(updSingleton);
                            return allUsers.zipWith(just, ZIPPER);
                        }
                    }
                })
                .observeOn(mainThread())
                .subscribe(new Action1<Portion>() {
                    @Override
                    public void call(Portion portion) {
                        delegate.newMessageAdded(portion);
                    }
                });
    }





    public void request2(final TdApi.Message lastMessage, @Nullable final TdApi.Message initMessage) {
        assertNull(request);
//        TdApi.Message lastMessage;
//        if (initMessage != null) {
//            lastMessage = initMessage;
//        } else {
//            lastMessage = getLastMessage();
//        }
        request = client.getMessages(chat.id, lastMessage.id, 0, Chat.LIMIT)
                .flatMap(new Func1<TdApi.Messages, Observable<? extends Portion>>() {
                    @Override
                    public Observable<? extends Portion> call(TdApi.Messages portion) {
                        atLeastOneRequestCompleted = true;
                        TdApi.Message[] messages = portion.messages;
                        users.clear();
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

                        if (users.isEmpty()) {
                            Portion res = new Portion(messageList, Collections.<TdApi.User>emptyList());
                            return Observable.just(res);
                        } else {
                            List<Observable<TdApi.User>> os = new ArrayList<>();
                            for (Integer uid : users) {
                                os.add(client.getUser(uid));
                            }
                            //request missing users and zip
                            Observable<List<TdApi.User>> allUsers = Observable.merge(os)
                                    .toList();

                            Observable<List<TdApi.Message>> messagesCopy = Observable.just(messageList);
                            return allUsers.zipWith(messagesCopy, ZIPPER);
                        }
                    }
                })
                .observeOn(mainThread());
    }

    private void getUIDs(TdApi.Message message) {
        assertTrue(message.fromId != 0);
        if (!delegate.hasUser(message.fromId)) {
            users.add(message.fromId);
        }
        if (message.forwardFromId != 0) {
            if (!delegate.hasUser(message.forwardFromId)) {
                users.add(message.forwardFromId);
            }
        }
    }

    public boolean isDownloadedAll() {
        return downloadedAll;
    }

    public boolean atLeastOneRequestCompleted() {
        return atLeastOneRequestCompleted;
    }

    interface AddListener {
        void historyAdded(Portion portion);
        void newMessageAdded(Portion portion);
        //called during flatmap so may be called even if we are unsubscribed
        boolean hasUser(Integer id);
    }


}
