package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.WindowManager;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import org.telegram.android.DpCalculator;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatDB implements UserHolder {



    private final int chatLimit;
    private final int messageLimit;
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
    public ChatDB(final Context ctx, final RXClient client, EmojiParser parser, DpCalculator calc) {
        this.client = client;
        this.parser = parser;
        prepareForUpdates();



        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay()
                .getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        int maxSize = Math.max(width, height);

        int aproxRowHeight = calc.dp(72);
        int limit = (int) (1.5 * maxSize / aproxRowHeight);
        chatLimit = Math.max(15, limit);

        int aproxMessageHeight = calc.dp(41);
        limit = (int) (1.5 * maxSize / aproxMessageHeight);
        messageLimit = Math.max(limit, 20);
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
                        updateChatMessageList(updateMessageContent.chatId);
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
                        updateChatMessageList(updateChatReadInbox.chatId);
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
                        updateChatMessageList(updateChatReadInbox.chatId);
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
                        updateChatMessageList(updateMessageDate.chatId);
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
                        updateChatMessageList(updateMessageId.chatId);
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
                        updateChatMessageList(messages.chatId);
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
                        getRxChat(updateNewMessage.message.chatId)
                                .handleNewMessage(updateNewMessage.message);
                        updateCurrentChatList();
                    }
                });
    }

    private void updateChatMessageList(long id){
        getRxChat(id)
                .updateCurrentMessageList();
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
        requestImpl(chatsList.size(), chatLimit, true);
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
        public final List<RxChat.ChatListItem> items;
        public final SparseArray<TdApi.User> us;

        public Portion(List<TdApi.Message> ms, List<TdApi.User> us, List<RxChat.ChatListItem> items) {
            this.ms = ms;
            this.items = items;
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

    public int getMessageLimit() {
        return messageLimit;
    }
}
