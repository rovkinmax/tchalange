package ru.korniltsev.telegram.chat;

import android.support.annotation.Nullable;
import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private final List<TdApi.Message> ms = new ArrayList<>();
    private final Map<Integer, TdApi.User> us = new ConcurrentHashMap<>();

    final RXClient client;
    final TdApi.Chat chat;
    //GuardedBy Tdlib Client thread
    private final Set<Integer> users = new HashSet<>();
    final AddListener listner;

    private Observable<Portion> request;
    private boolean downloadedAll;

    public MessagesHolder(RXClient client, TdApi.Chat chat, AddListener listner) {
        this.client = client;
        this.chat = chat;
        this.listner = listner;
    }

    public void add(TdApi.Message topMessage, @Nullable TdApi.User u) {
        ms.add(topMessage);
        if (u != null) {
            us.put(u.id, u);
        }
    }

    public boolean isEmpty() {
        return ms.isEmpty();
    }

    public Portion getMs() {
        return new Portion(ms, us);
    }

    public void add(Portion messages) {
        for (TdApi.Message message : messages.ms) {
            ms.add(message);
        }
        this.us.putAll(messages.us);
    }

    public TdApi.Message getLastMessage() {
        return ms.get(ms.size() - 1);
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
                    add(portion);
                    listner.messagesAdded(portion);
                }
            }
        });
    }

    //todo delete this shit ASAP
    public static class Portion {
        final List<TdApi.Message> ms;
        final Map<Integer, TdApi.User> us;

        public Portion(List<TdApi.Message> ms, List<TdApi.User> us) {
            this.ms = ms;
            this.us = new HashMap<>();
            for (TdApi.User u : us) {
                this.us.put(u.id, u);
            }
            Log.d("MessagesHolder", "us.size" + us.size());
        }

        public Portion(List<TdApi.Message> ms, Map<Integer, TdApi.User> us) {
            this.ms = ms;
            this.us = us;
        }
    }

    public void request(@Nullable final TdApi.Message initMessage) {
        assertNull(request);
        TdApi.Message lastMessage;
        if (initMessage != null) {
            lastMessage = initMessage;
        } else {
            lastMessage = getLastMessage();
        }
        request = client.getMessages(chat.id, lastMessage.id, 0, Chat.LIMIT)
                .flatMap(new Func1<TdApi.Messages, Observable<? extends Portion>>() {
                    @Override
                    public Observable<? extends Portion> call(TdApi.Messages portion) {
                        TdApi.Message[] messages = portion.messages;
                        users.clear();
                        for (TdApi.Message message : messages) {
                            getUIDs(message);
                        }
                        if (initMessage != null) {
                            getUIDs(initMessage);
                        }
                        List<Observable<TdApi.User>> os = new ArrayList<>();
                        for (Integer uid : users) {
                            os.add(client.getUser(uid));
                        }

                        final List<TdApi.Message> messageList = new ArrayList<>();
                        if (initMessage != null) {
                            messageList.add(initMessage);
                        }
                        for (TdApi.Message m : portion.messages) {
                            messageList.add(m);
                        }

                        if (users.isEmpty()) {
                            Portion res = new Portion(messageList, Collections.<TdApi.User>emptyList());
                            return Observable.just(res);
                        } else {
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
        if (!us.containsKey(message.fromId)) {
            users.add(message.fromId);
        }
        if (message.forwardFromId != 0) {
            if (!us.containsKey(message.forwardFromId)) {
                users.add(message.forwardFromId);
            }
        }
    }

    public boolean isDownloadedAll() {
        return downloadedAll;
    }

    interface AddListener {
        void messagesAdded(Portion portion);
    }
}
