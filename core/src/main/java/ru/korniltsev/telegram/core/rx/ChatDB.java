package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.WindowManager;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final Context ctx;
    final RXClient client;
    final NotificationManager nm;
    final EmojiParser parser;
    private Observable<ChatPortion> chatsRequest;
    private boolean downloadedAllChats;
    private boolean atLeastOneResponseReturned;
    private Observable<TdApi.UpdateMessageId> messageIdsUpdates;

    public Observable<TdApi.UpdateMessageId> getMessageIdsUpdates(final long id) {
        return messageIdsUpdates.filter(new Func1<TdApi.UpdateMessageId, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateMessageId updateMessageId) {
                return updateMessageId.chatId == id;
            }
        });
    }

    @Inject
    public ChatDB(final Context ctx, final RXClient client, EmojiParser parser, DpCalculator calc, NotificationManager nm, RXAuthState auth) {
        this.ctx = ctx;
        this.client = client;
        this.parser = parser;
        this.nm = nm;
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
        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateLogout){
                            synchronized (userIdToUser){
                                userIdToUser.clear();
                            }
                            chatIdToRxChat.clear();
                            chatsList.clear();
                            downloadedAllChats = false;
                            atLeastOneResponseReturned = false;
                        }
                    }
                });
    }



    private void prepareForUpdates() {
        prepareForUpdateNewMessage();
        prepareForUpdateDeleteMessages();
        prepareForUpdateMessageId();
//        prepareForUpdateMessageDate();
        //todo this 3 ones are probably needed
//        prepareForUpdateChatReadInbox();
        prepareForUpdateChatReadOutbox();
        prepareForUpdateUserStatus();
        prepareForUpdateMessageContent();

//        prepareForUpdateChatTitle();
//        prepareForUpdateChatParticipantsCount();
    }

    Map<Integer, TdApi.UserStatus> userIdToUserStatus = new HashMap<>();

    private void prepareForUpdateUserStatus() {
        client.usersStatus()
                .subscribe(new ObserverAdapter<TdApi.UpdateUserStatus>() {
                    @Override
                    public void onNext(TdApi.UpdateUserStatus response) {
                        userIdToUserStatus.put(response.userId, response.status);
                    }
                });
    }

    public TdApi.UserStatus getUserStatus(TdApi.User u) {
        TdApi.UserStatus updatedStatus = userIdToUserStatus.get(u.id);
        if (updatedStatus == null) {
            return u.status;
        } else {
            return updatedStatus;
        }
    }

    private void prepareForUpdateMessageContent() {
        client.updateMessageContent()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateMessageContent>() {
                    @Override
                    public void onNext(TdApi.UpdateMessageContent updateMessageContent) {
                        getRxChat(updateMessageContent.chatId)
                                .updateContent(updateMessageContent);

                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateChatReadOutbox() {
        client.updateChatReadOutbox()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateChatReadOutbox>() {
                    @Override
                    public void onNext(TdApi.UpdateChatReadOutbox response) {
//                        updateChatMessageList(updateChatReadInbox.chatId);
                        updateCurrentChatList();
                    }
                });
    }

//    private void prepareForUpdateChatReadInbox() {
//        client.updateChatReadInbox()
//                .observeOn(mainThread())
//                .subscribe(new ObserverAdapter<TdApi.UpdateChatReadInbox>() {
//                    @Override
//                    public void onNext(TdApi.UpdateChatReadInbox updateChatReadInbox) {
//                        updateChatMessageList(updateChatReadInbox.chatId);
//                        updateCurrentChatList();
//                    }
//                });
//    }

//    private void prepareForUpdateMessageDate() {
//        client.updateMessageDate()
//                .observeOn(mainThread())
//                .subscribe(new ObserverAdapter<TdApi.UpdateMessageDate>() {
//                    @Override
//                    public void onNext(TdApi.UpdateMessageDate updateMessageDate) {
//                        updateChatMessageList(updateMessageDate.chatId);
//                        updateCurrentChatList();
//                    }
//                });
//    }

    private void prepareForUpdateMessageId() {
        messageIdsUpdates = client.updateMessageId()
                .observeOn(mainThread())
                .map(new Func1<TdApi.UpdateMessageId, TdApi.UpdateMessageId>() {
                    @Override
                    public TdApi.UpdateMessageId call(TdApi.UpdateMessageId updateMessageId) {
                        getRxChat(updateMessageId.chatId)
                                .updateMessageId(updateMessageId);
                        updateCurrentChatList();
                        return updateMessageId;
                    }
                });
        messageIdsUpdates.subscribe();
    }

    private void prepareForUpdateDeleteMessages() {
        client.updateDeleteMessages()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateDeleteMessages>() {
                    @Override
                    public void onNext(TdApi.UpdateDeleteMessages messages) {
                        getRxChat(messages.chatId)
                                .deleteMessageImpl(messages.messages);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateNewMessage() {
        client.updateNewMessages()
                .map(new Func1<TdApi.UpdateNewMessage, TdApi.UpdateNewMessage>() {
                    @Override
                    public TdApi.UpdateNewMessage call(TdApi.UpdateNewMessage updateNewMessage) {
                        parser.parse(updateNewMessage.message);
                        return updateNewMessage;
                    }
                })
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateNewMessage>() {
                    @Override
                    public void onNext(TdApi.UpdateNewMessage updateNewMessage) {
                        getRxChat(updateNewMessage.message.chatId)
                                .handleNewMessage(updateNewMessage.message);
                        nm.notifyNewMessage(updateNewMessage.message);
                        updateCurrentChatList();
                    }
                });
    }

//    private void updateChatMessageList(long id){
//        getRxChat(id)
//                .updateCurrentMessageList();
//    }


    public void updateCurrentChatList() {
        checkMainThread();
        if (chatsRequest != null) {
            //todo
        } else {
            requestImpl(0, chatsList.size() + 1, false);
        }
    }

    public void saveUsers(SparseArray<TdApi.User> us) {
        for(int i = 0; i < us.size(); i++) {
            TdApi.User obj = us.get(
                    us.keyAt(i));
            saveUser(obj);
        }
    }

    class  ChatPortion {
        final TdApi.Chats  cs ;
        final SparseArray<TdApi.User> us;

        public ChatPortion(TdApi.Chats cs, SparseArray<TdApi.User> us) {
            this.cs = cs;
            this.us = us;
        }
    }
    //request new portion
    public void requestPortion() {
        requestImpl(chatsList.size(), chatLimit, true);
    }

    final Set<Integer> tmpIds = new HashSet<>();
    private void requestImpl(int offset, int limit, final boolean historyRequest) {
        Assert.assertNull(chatsRequest);
        chatsRequest = client.getChats(offset, limit)
                .flatMap(new Func1<TdApi.Chats, Observable<ChatPortion>>() {
                    @Override
                    public Observable<ChatPortion> call(TdApi.Chats chats) {
                        tmpIds.clear();
                        checkNotMainThread();
                        for (TdApi.Chat chat : chats.chats) {
                            parser.parse(chat.topMessage);
                            tmpIds.add(chat.topMessage.fromId);
                            if (chat.topMessage.forwardFromId != 0) {
                                tmpIds.add(chat.topMessage.fromId);
                            }
                        }
                        List<Observable<TdApi.User>> us = new ArrayList<Observable<TdApi.User>>();
                        tmpIds.remove(0);//todo who ads it here
                        for (Integer id : tmpIds) {
                            us.add(client.getUser(id));
                        }
                        Observable<List<TdApi.User>> users = Observable.merge(us)
                                .toList();
                        return Observable.zip(users, Observable.just(chats), new Func2<List<TdApi.User>, TdApi.Chats, ChatPortion>() {
                            @Override
                            public ChatPortion call(List<TdApi.User> users, TdApi.Chats chats) {
                                SparseArray<TdApi.User> us = new SparseArray<>();
                                for (TdApi.User user : users) {
                                    us.put(user.id, user);
                                }
                                ChatPortion chatPortion = new ChatPortion(chats, us);
                                return chatPortion;
                            }
                        });
                    }
                })
                .observeOn(mainThread());



        chatsRequest.subscribe(new ObserverAdapter<ChatPortion>() {
            @Override
            public void onNext(ChatPortion p) {
                saveUsers(p.us);
                atLeastOneResponseReturned = true;
                chatsRequest = null;
                if (p.cs.chats.length == 0) {
                    downloadedAllChats = true;
                }
                List<TdApi.Chat> csList = Arrays.asList(p.cs.chats);
                if (!historyRequest) {
                    chatsList.clear();
                }
                chatsList.addAll(csList);
                nm.updateNotificationScopes(chatsList);
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

    @Override
    public Context getContext() {
        return ctx;
    }

    public static class Portion {
        public final List<TdApi.Message> ms;
//        public final List<RxChat.ChatListItem> items;
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

    public int getMessageLimit() {
        return messageLimit;
    }
}
