package ru.korniltsev.telegram.core.rx;

import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class RxChatDB implements UserHolder {

    public static final Func2<List<TdApi.User>, List<TdApi.Message>, Portion> ZIPPER = new Func2<List<TdApi.User>, List<TdApi.Message>, Portion>() {
        @Override
        public Portion call(List<TdApi.User> users, List<TdApi.Message> messages) {
            return new Portion(messages, users);
        }
    };

    static final int LIMIT = 10;
    //guarded by ui thread
    final List<TdApi.Chat> chatsList = new ArrayList<>();
    //guarded by ui thread
    final PublishSubject<List<TdApi.Chat>> currentChatList = PublishSubject.create();


    /*

                        UpdateDeleteMessages extends Update {
                        UpdateNewMessage extends Update

                        UpdateMessageId extends Update {
                        UpdateMessageDate extends Update {


                        UpdateChatReadInbox extends Update {
                        UpdateChatReadOutbox extends Update {
                        UpdateMessageContent extends Update {
                        UpdateChatTitle extends Update {
                        UpdateChatParticipantsCount extends Update {

                        todo what is this UpdateNotificationSettings extends Update {


     */

    final RXClient client;
    EmojiParser parser;
    private Observable<TdApi.Chats> chatsRequest;
    private boolean downloadedAllChats;
    private boolean atLeastOneResponseReturned;

    @Inject
    public RxChatDB(final RXClient client, EmojiParser parser) {
        this.client = client;
        this.parser = parser;
        prepareForUpdates();
    }

    private void prepareForUpdates() {
        prepareForUpdateNewMessage();
        prepareForUpdateDeleteMessages();
        prepareForUpdateMessageId();
        prepareForUpdateMessageDate();
        //todo this 3 ones are probably needed
//        prepareForUpdateChatReadInbox();
//        prepareForUpdateChatReadOutbox();
//        prepareForUpdateMessageContent();

//        prepareForUpdateChatTitle();
//        prepareForUpdateChatParticipantsCount();
    }

    private void prepareForUpdateMessageContent() {
        client.updateMessageContent()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateMessageContent>() {
                    @Override
                    public void call(TdApi.UpdateMessageContent updateMessageContent) {
                        updateChatMessageList(updateMessageContent.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateChatReadOutbox() {
        client.updateChatReadOutbox()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateChatReadOutbox>() {
                    @Override
                    public void call(TdApi.UpdateChatReadOutbox updateChatReadInbox) {
                        updateChatMessageList(updateChatReadInbox.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateChatReadInbox() {
        client.updateChatReadInbox()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateChatReadInbox>() {
                    @Override
                    public void call(TdApi.UpdateChatReadInbox updateChatReadInbox) {
                        updateChatMessageList(updateChatReadInbox.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateMessageDate() {
        client.updateMessageDate()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateMessageDate>() {
                    @Override
                    public void call(TdApi.UpdateMessageDate updateMessageDate) {
                        updateChatMessageList(updateMessageDate.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateMessageId() {
        client.updateMessageId()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateMessageId>() {
                    @Override
                    public void call(TdApi.UpdateMessageId updateMessageId) {
                        getRxChat(updateMessageId.chatId)
                                .updateMessageId(updateMessageId);
                        updateChatMessageList(updateMessageId.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateDeleteMessages() {
        client.updateDeleteMessages()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateDeleteMessages>() {
                    @Override
                    public void call(TdApi.UpdateDeleteMessages messages) {
                        updateChatMessageList(messages.chatId, false);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateNewMessage() {
        client.updateNewMessages()
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateNewMessage>() {
                    @Override
                    public void call(TdApi.UpdateNewMessage updateNewMessage) {
                        updateChatMessageList(updateNewMessage.message.chatId, true);
                        updateCurrentChatList();
                    }
                });
    }

    private void updateChatMessageList(long id, boolean newMessage){
        getRxChat(id)
                .updateCurrentMessageList(newMessage);
    }


    private void updateCurrentChatList() {
        checkMainThread();
        if (chatsRequest != null) {
            //todo
        } else {
            requestImpl(0, chatsList.size() + 1, false);
        }
    }



    //request new portion
    public void requestPortion() {
        requestImpl(chatsList.size(), LIMIT, true);
    }

    private void requestImpl(int offset, int limit, final boolean historyRequest) {
        Assert.assertNull(chatsRequest);
        chatsRequest = client.getChats(offset, limit)
                .map(new Func1<TdApi.Chats, TdApi.Chats>() {
                    @Override
                    public TdApi.Chats call(TdApi.Chats chats) {
                        checkNotMainThread();
                        for (TdApi.Chat c : chats.chats) {
                            parser.parse(c.topMessage);
                        }
                        return chats;
                    }
                })
        .observeOn(mainThread());

        chatsRequest.subscribe(new Action1<TdApi.Chats>() {
            @Override
            public void call(TdApi.Chats chats) {
                atLeastOneResponseReturned = true;
                chatsRequest = null;
                if (chats.chats.length == 0) {
                    downloadedAllChats = true;
                }
                // todo parse smiles and refs
                List<TdApi.Chat> csList = Arrays.asList(chats.chats);
                if (!historyRequest) {
                    chatsList.clear();
                }
                chatsList.addAll(csList);
                currentChatList.onNext(chatsList);
            }
        });
    }



    public Observable<List<TdApi.Chat>> chatList() {
        checkMainThread();
        return currentChatList;
    }

    public List<TdApi.Chat> getAllChats() {
        checkMainThread();
        return chatsList;
    }

    public boolean isRequestInProgress() {
        checkMainThread();
        return chatsRequest != null;
    }

    public RxChat getRxChat(long id) {
        checkMainThread();
        RxChat rxChat = chatIdToRxChat.get(id);
        if (rxChat == null) {
            rxChat = new RxChat(id, client, this);
            chatIdToRxChat.put(id, rxChat);
            return rxChat;
        }
        return rxChat;
    }

    //guarded by ui thread
    private final LongSparseArray<RxChat> chatIdToRxChat = new LongSparseArray<>();

    //guarded by userIdToUser
    private final SparseArrayCompat<TdApi.User> userIdToUser = new SparseArrayCompat<>();

    @Override
    public boolean hasUserWith(int id) {
        return getUser(id) != null;
    }

    @Override
    public TdApi.User getUser(int id) {
        synchronized (userIdToUser) {
            return userIdToUser.get(id);
        }
    }

    @Override
    public void saveUser(TdApi.User u) {
        synchronized (userIdToUser) {
            userIdToUser.put(u.id, u);
        }
    }

    public static class Portion {
        public final List<TdApi.Message> ms;
        public final SparseArray<TdApi.User> us;

        public Portion(List<TdApi.Message> ms, List<TdApi.User> us) {
            this.ms = ms;
            this.us = new SparseArray<>();
            for (TdApi.User u : us) {
                this.us.put(u.id, u);
            }
        }
    }

    public boolean isDownloadedAllChats() {
        checkMainThread();
        return downloadedAllChats;
    }

    public boolean isAtLeastOneResponseReturned() {
        checkMainThread();
        return atLeastOneResponseReturned;
    }
}
