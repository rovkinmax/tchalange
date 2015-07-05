package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.addAll;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class RxChat implements UserHolder {

    public static final int MESSAGE_STATE_READ = 0;
    public static final int MESSAGE_STATE_SENT = 1;
    public static final int MESSAGE_STATE_NOT_SENT = 2;
    private static final int MSG_WITHOUT_VALID_ID = 1000000000;

    //    public static int compareInt(int lhs, int rhs) {
    //        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    //    }

    //    private SortedSet<TdApi.Message> ms = new TreeSet<>(new Comparator<TdApi.Message>() {
    //        @Override
    //        public int compare(TdApi.Message lhs, TdApi.Message rhs) {
    //            int dateCompare = -compareInt(lhs.date, rhs.date);
    //            if (dateCompare == 0) {
    //                return -compareInt(lhs.id, rhs.id);
    //            }
    //            return dateCompare;
    //        }
    //    });

    final List<TdApi.Message> data = new ArrayList<>();
    //    todo

    public final Func2<List<TdApi.User>, List<TdApi.Message>, ChatDB.Portion> ZIPPER = new Func2<List<TdApi.User>, List<TdApi.Message>, ChatDB.Portion>() {
        @Override
        public ChatDB.Portion call(List<TdApi.User> users, List<TdApi.Message> messages) {
            return new ChatDB.Portion(messages, users);
        }
    };

    private Func1<TdApi.TLObject, TdApi.Message> CAST_TO_MESSAGE_AND_PARSE_EMOJI = new Func1<TdApi.TLObject, TdApi.Message>() {
        @Override
        public TdApi.Message call(TdApi.TLObject tlObject) {
            TdApi.Message msg = (TdApi.Message) tlObject;
            holder.parser.parse(msg);
            return msg;
        }
    };
    private ObserverAdapter<TdApi.Message> HANDLE_NEW_MESSAGE = new ObserverAdapter<TdApi.Message>() {
        @Override
        public void onNext(final TdApi.Message tlObject) {
            handleNewMessage(tlObject);
        }
    };

    final long id;
    final RXClient client;
    public final ChatDB holder;

    //    private final PublishSubject<List<TdApi.Message>> subject = PublishSubject.create();
    private PublishSubject<TdApi.Message> newMessage = PublishSubject.create();
    private PublishSubject<HistoryResponse> historySubject = PublishSubject.create();
    private PublishSubject<DeletedMessages> deletedMessagesSubject = PublishSubject.create();
    private PublishSubject<TdApi.Message> contentChanged = PublishSubject.create();

    private ThreadLocal<Set<Integer>> tmpUIDs = new ThreadLocal<Set<Integer>>() {
        @Override
        protected Set<Integer> initialValue() {
            return new HashSet<>();
        }
    };

    private boolean downloadedAll;
    private Subscription subscription = Subscriptions.empty();
    private Observable<ChatDB.Portion> request;

    public RxChat(long id, RXClient client, ChatDB holder) {
        this.id = id;
        this.client = client;
        this.holder = holder;
    }

    public Observable<TdApi.Message> getNewMessage() {
        return newMessage;
    }

    public boolean isRequestInProgress() {
        return request != null;
    }

    //    public void requestNewPotion() {
    //        List<TdApi.Message> messages = getMessages();
    //        TdApi.Message lastMessage = messages.get(messages.size() - 1);
    //        requestImpl(lastMessage, null, true, holder.getMessageLimit(), 0);
    //    }

    //    private void requestImpl(TdApi.Message lastMessage, final TdApi.Message initMessage, final boolean historyRequest, int limit, int offset) {
    //        assertNull(request);
    //        request = client.getMessages(id, lastMessage.id, offset, limit)
    //                .flatMap(new GetUsers(initMessage))
    //                .observeOn(mainThread());
    //
    //        subscription = request.subscribe(new ObserverAdapter<ChatDB.Portion>() {
    //            @Override
    //            public void onNext(ChatDB.Portion portion) {
    //                checkMainThread();
    //                request = null;
    //                if (portion.ms.isEmpty()) {
    //                    downloadedAll = true;
    //                } else {
    //
    //                    SparseArray<TdApi.User> us = portion.us;
    //                    holder.saveUsers(us);
    //                    if (!historyRequest) {
    //                        ms.clear();
    //                    }
    //                    ms.addAll(portion.ms);
    //                    subject.onNext(getMessages());
    //                }
    //            }
    //        });
    //    }

    @NonNull
    private Observable<List<TdApi.User>> requestUsers(Set<Integer> ids) {
        Observable<List<TdApi.User>> allUsers;
        List<Observable<TdApi.User>> os = new ArrayList<>();
        for (Integer uid : ids) {
            os.add(client.getUser(uid));
        }
        //request missing users and zip
        allUsers = Observable.merge(os)
                .toList();
        return allUsers;
    }

    private void getUIDs(TdApi.Message message) {
        //todo pas set as arguments
        assertTrue(message.fromId != 0);
        if (!hasUserWith(message.fromId)) {
            tmpUIDs.get().add(message.fromId);
        }
        if (message.forwardFromId != 0) {
            if (!hasUserWith(message.forwardFromId)) {
                tmpUIDs.get().add(message.forwardFromId);
            }
        }
        if (message.message instanceof TdApi.MessageContact) {
            TdApi.MessageContact c = (TdApi.MessageContact) message.message;
            tmpUIDs.get().add(c.userId);
        }
    }

    //    public Observable<List<TdApi.Message>> messageList() {
    //        return subject;
    //    }

    public List<TdApi.Message> getMessages() {
        return data;
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

    @Override
    public Context getContext() {
        return holder.getContext();
    }

    //    public void updateCurrentMessageList() {
    //        checkMainThread();
    //        if (ms.isEmpty()) {
    //            return;
    //        }
    //        if (request == null) {
    //            int offset = -1;
    //            List<TdApi.Message> messages = getMessages();
    //            int size = messages.size();
    //            requestImpl(messages.get(0), null, false, size, offset);
    //        } else {
    //
    //        }
    //    }

    public void handleNewMessage(final TdApi.Message tlObject) {

        sentPhotoHack(tlObject);

        tmpUIDs.get().clear();
        getUIDs(tlObject);
        if (tmpUIDs.get().isEmpty()) {
            addNewMessageAndDispatch(tlObject);
        } else {
            requestUsers(tmpUIDs.get())
                    .observeOn(mainThread())
                    .subscribe(new ObserverAdapter<List<TdApi.User>>() {
                        @Override
                        public void onNext(List<TdApi.User> response) {
                            for (int i = 0; i < response.size(); i++) {
                                TdApi.User user = response.get(i);
                                holder.saveUser(user);
                            }
                            addNewMessageAndDispatch(tlObject);
                        }
                    });
        }
    }

    //hack to prevent reloading of newly added image
    final SparseArray<TdApi.FileLocal> sentMessageIdToImageLink = new SparseArray<>();

    private void sentPhotoHack(TdApi.Message msg) {
        if (msg.message instanceof TdApi.MessagePhoto
                && msg.id >= MSG_WITHOUT_VALID_ID) {
            final TdApi.Photo p = ((TdApi.MessagePhoto) msg.message).photo;
            if (p.photos.length == 1) {
                final TdApi.PhotoSize photo = p.photos[0];
                if (photo.type.equals("i") && photo.photo instanceof TdApi.FileLocal) {
                    sentMessageIdToImageLink.put(msg.id, (TdApi.FileLocal) photo.photo);
                }
            }
        }
    }

    @Nullable public TdApi.FileLocal getSentImage(TdApi.Message msg) {
        return sentMessageIdToImageLink.get(msg.id);
    }

    private void addNewMessageAndDispatch(TdApi.Message msg) {
        data.add(0, msg);
        newMessage.onNext(msg);
    }

    public void deleteHistory() {
        client.sendRx(new TdApi.DeleteChatHistory(id))
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        data.clear();
                        deletedMessagesSubject.onNext(ALL_DELETED_MESSAGES);
                    }
                });
    }

    public static final DeletedMessages ALL_DELETED_MESSAGES = new DeletedMessages();

    final SparseArray<TdApi.UpdateMessageId> newIdToUpdate = new SparseArray<>();
    final SparseArray<TdApi.UpdateMessageId> oldIdToUpdate = new SparseArray<>();

    public void updateMessageId(TdApi.UpdateMessageId upd) {
        //never really changes id of the TdApi.Message
        newIdToUpdate.put(upd.newId, upd);
        oldIdToUpdate.put(upd.oldId, upd);
    }

    public TdApi.UpdateMessageId getUpdForNewId(int newId) {
        return newIdToUpdate.get(newId);
    }

    public TdApi.UpdateMessageId getUpdForOldId(int oldId) {
        return oldIdToUpdate.get(oldId);
    }

    public Observable<TdApi.TLObject> deleteMessage(final int messageId) {
        return client.sendCachedRXUI(new TdApi.DeleteMessages(id, new int[]{messageId}))
                .map(new Func1<TdApi.TLObject, TdApi.TLObject>() {
                    @Override
                    public TdApi.TLObject call(TdApi.TLObject tlObject) {
                        checkMainThread();
                        deleteMessageImpl(new int[]{messageId});
                        return tlObject;
                    }
                });
    }

    public void deleteMessageImpl(int[] ms) {
        //todo O(n*m)
        final ArrayList<TdApi.Message> deletedMessages = new ArrayList<>();
        for (TdApi.Message message : data) {
            for (int messageId : ms) {
                if (message.id == messageId) {
                    deletedMessages.add(message);
                    break;
                }
            }
        }
        data.removeAll(deletedMessages);
        deletedMessagesSubject.onNext(new DeletedMessages(deletedMessages));
    }

    public void sendSticker(String stickerFilePath) {
        TdApi.InputMessageSticker content = new TdApi.InputMessageSticker(stickerFilePath);
        sendMessageImpl(content);
    }

    public void sendMessage(String text) {
        TdApi.InputMessageText content = new TdApi.InputMessageText(text);
        sendMessageImpl(content);
    }

    private void sendMessageImpl(TdApi.InputMessageContent content) {
        client.sendRx(new TdApi.SendMessage(id, content))
                .map(CAST_TO_MESSAGE_AND_PARSE_EMOJI)
                .observeOn(mainThread())
                .subscribe(HANDLE_NEW_MESSAGE);
    }

    public void hackToReadTheMessage(TdApi.Message msg) {
        client.sendRx(new TdApi.GetChatHistory(id, msg.id, -1, 1))
                .subscribe(new ObserverAdapter<TdApi.TLObject>());
    }

    public void sendImage(String imageFilePath) {

        sendMessageImpl(new TdApi.InputMessagePhoto(imageFilePath));
    }

    public void clear() {
        request = null;
        subscription.unsubscribe();
        data.clear();
        downloadedAll = false;
    }

    public void initialRequest(final TdApi.Chat chat) {
        assertNull(request);
        if (chat.topMessage.id > 0) {
            //            parameter from_id must be positive GetChatHistory {
            //                chatId = 98255217
            //                fromId = 0
            //                offset = 0
            //                limit = 23
            //            }
            request = client.getMessages(id, chat.topMessage.id, 0, holder.getMessageLimit())
                    .compose(new GetUsers2(chat.topMessage));
            request.subscribe(new HistoryObserver(false));
        }
    }

    public void requestNewPotion() {
        TdApi.Message lastMessage = getLastMessage();
        if (lastMessage == null) {
            return;
        }
        assertNull(request);
        request = client.getMessages(id, lastMessage.id, 0, holder.getMessageLimit())
                .compose(new GetUsers2(null));
        request.subscribe(new HistoryObserver(false));
    }

    @Nullable
    private TdApi.Message getLastMessage() {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }

    public void requestUntilLastUnread(TdApi.Chat chat) {
        assertNull(request);
        final int until = chat.lastReadInboxMessageId;
        if (until == chat.topMessage.id) {
            initialRequest(chat);
            return;
        }
        request = requestUntilLastUnreadRecursive(chat.topMessage.id, until)
                .compose(new GetUsers2(chat.topMessage));
        request.subscribe(new HistoryObserver(true));
    }

    private Observable<TdApi.Messages> requestUntilLastUnreadRecursive(int messageId, final int untilId) {
        return client.getMessages(id, messageId, 0, holder.getMessageLimit())
                .flatMap(new Func1<TdApi.Messages, Observable<TdApi.Messages>>() {
                    @Override
                    public Observable<TdApi.Messages> call(TdApi.Messages messages) {
                        final Observable<TdApi.Messages> just = Observable.just(messages);
                        if (contains(messages, untilId)
                                || messages.messages.length == 0) {
                            return just;
                        } else {
                            final TdApi.Message lastMessage = messages.messages[messages.messages.length - 1];
                            final Observable<TdApi.Messages> next = requestUntilLastUnreadRecursive(lastMessage.id, untilId);
                            return just.zipWith(next, new Func2<TdApi.Messages, TdApi.Messages, TdApi.Messages>() {
                                @Override
                                public TdApi.Messages call(TdApi.Messages o, TdApi.Messages o2) {
                                    final ArrayList<TdApi.Message> res = new ArrayList<>(o.messages.length + o2.messages.length);
                                    addAll(res, o.messages);
                                    addAll(res, o2.messages);
                                    return new TdApi.Messages(res.toArray(new TdApi.Message[res.size()]));
                                }
                            });
                        }
                    }
                });
    }

    private boolean contains(TdApi.Messages messages, int untilId) {
        for (TdApi.Message it : messages.messages) {
            if (it.id == untilId) {
                return true;
            }
        }
        return false;
    }

    public void updateContent(TdApi.UpdateMessageContent upd) {
        for (TdApi.Message message : data) {
            if (message.id == upd.messageId) {
                message.message = upd.newContent;
                contentChanged.onNext(message);
                return;
            }
        }
    }

    public static abstract class ChatListItem {

    }

    public static class NewMessagesItem extends ChatListItem {
        public final int newMessagesCount;
        public final long id;

        public NewMessagesItem(int newMessagesCount, long id) {
            this.newMessagesCount = newMessagesCount;
            this.id = id;
        }
    }

    public static class MessageItem extends ChatListItem {
        public final TdApi.Message msg;

        public MessageItem(TdApi.Message msg) {
            this.msg = msg;
        }
    }

    public static class DaySeparatorItem extends ChatListItem {
        public final long id;
        public final DateTime day;//millis

        public DaySeparatorItem(long id, DateTime day) {
            this.id = id;
            this.day = day;
        }
    }

    public int getMessageState(TdApi.Message msg, long lastReadOutbox, int myId) {
        if (myId != msg.fromId) {
            return MESSAGE_STATE_READ;
        }
        TdApi.UpdateMessageId upd = getUpdForOldId(msg.id);
        if (msg.id >= MSG_WITHOUT_VALID_ID && upd == null) {
            return MESSAGE_STATE_NOT_SENT;
        } else {
            //message sent
            int id = msg.id;
            if (id >= MSG_WITHOUT_VALID_ID) {
                id = upd.newId;
            }
            if (lastReadOutbox < id) {
                return MESSAGE_STATE_SENT;
            } else {
                return MESSAGE_STATE_READ;
            }
        }
    }

    private class GetUsers implements Func1<TdApi.Messages, Observable<? extends ChatDB.Portion>> {
        private final TdApi.Message initMessage;

        public GetUsers(TdApi.Message initMessage) {
            this.initMessage = initMessage;
        }

        @Override
        public Observable<? extends ChatDB.Portion> call(TdApi.Messages portion) {

            checkNotMainThread();
            TdApi.Message[] messages = portion.messages;
            tmpUIDs.get().clear();//todo boxing
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

            if (tmpUIDs.get().isEmpty()) {
                ChatDB.Portion res = new ChatDB.Portion(messageList, Collections.<TdApi.User>emptyList());
                return Observable.just(res);
            } else {
                Observable<List<TdApi.User>> allUsers = requestUsers(tmpUIDs.get());

                Observable<List<TdApi.Message>> messagesCopy = Observable.just(messageList);
                return allUsers.zipWith(messagesCopy, ZIPPER);
            }
        }
    }

    public Observable<HistoryResponse> history() {
        return historySubject;
    }

    public class HistoryResponse {
        public final List<TdApi.Message> ms;
        /**
         * true если зашли в чат с непрочитанными сообщениями
         */
        public final boolean showUnreadMessages;

        public HistoryResponse(List<TdApi.Message> ms, boolean showUnreadMessages) {
            this.ms = ms;
            this.showUnreadMessages = showUnreadMessages;
        }
    }

    private class GetUsers2 implements Observable.Transformer<TdApi.Messages, ChatDB.Portion> {
        private final TdApi.Message topMessage;

        public GetUsers2(TdApi.Message topMessage) {
            this.topMessage = topMessage;
        }

        @Override
        public Observable<ChatDB.Portion> call(Observable<TdApi.Messages> original) {
            return original.flatMap(new GetUsers(topMessage))
                    .observeOn(mainThread());
        }
    }

    private class HistoryObserver extends ObserverAdapter<ChatDB.Portion> {
        final boolean unreadRequest;

        public HistoryObserver(boolean unreadRequest) {
            this.unreadRequest = unreadRequest;
        }

        @Override
        public void onNext(ChatDB.Portion response) {
            checkMainThread();
            request = null;
            if (response.ms.isEmpty()) {
                downloadedAll = true;
            } else {
                holder.saveUsers(response.us);
                data.addAll(response.ms);
                historySubject.onNext(new HistoryResponse(response.ms, unreadRequest));
            }
        }
        //todo on error
    }

    public static class DeletedMessages {
        public final boolean all;
        public final List<TdApi.Message> ms;

        public DeletedMessages() {
            this.all = true;
            ms = Collections.emptyList();
        }

        public DeletedMessages(List<TdApi.Message> ms) {
            this.all = false;
            this.ms = ms;
        }
    }

    public Observable<DeletedMessages> getDeletedMessagesSubject() {
        return deletedMessagesSubject;
    }

    public Observable<TdApi.Message> getContentChanged() {
        return contentChanged;
    }
}
