package ru.korniltsev.telegram.core.rx;

import android.util.Log;
import android.util.SparseArray;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
    public static int compareInt(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }
    private SortedSet<TdApi.Message> ms = new TreeSet<>(new Comparator<TdApi.Message>() {
        @Override
        public int compare(TdApi.Message lhs, TdApi.Message rhs) {
            int dateCompare = -compareInt(lhs.date, rhs.date);
            if (dateCompare == 0) {
                return -compareInt(lhs.id, rhs.id);
            }
            return dateCompare;
        }
    });

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
        public void onNext(TdApi.Message tlObject) {
            handleNewMessage(tlObject);
        }
    };


    final long id;
    final RXClient client;
    public final ChatDB holder;


    private final PublishSubject<List<TdApi.Message>> subject = PublishSubject.create();
    private PublishSubject<TdApi.Message> newMessage = PublishSubject.create();

    private Observable<ChatDB.Portion> request;
    private Set<Integer> tmpUIDs = new HashSet<>();

    private boolean atLeastOneRequestCompleted = false;
    private boolean downloadedAll;


    public RxChat(long id, RXClient client, ChatDB holder) {
        this.id = id;
        this.client = client;
        this.holder = holder;
    }

    public Observable<TdApi.Message> getNewMessage() {
        return newMessage;
    }

    public boolean atLeastOneRequestCompleted() {
        return atLeastOneRequestCompleted;
    }

    public boolean isRequestInProgress() {
        return request != null;
    }

    public void request2(TdApi.Message lastMessage, final TdApi.Message initMessage) {
        requestImpl(lastMessage, initMessage, true, holder.getMessageLimit(), 0);
    }

    public void requestNewPotion() {
        List<TdApi.Message> messages = getMessages();
        TdApi.Message lastMessage = messages.get(messages.size() - 1);
        requestImpl(lastMessage, null, true, holder.getMessageLimit(), 0);
    }

    private void requestImpl(TdApi.Message lastMessage, final TdApi.Message initMessage, final boolean historyRequest, int limit, int offset) {
        assertNull(request);
        request = client.getMessages(id, lastMessage.id, offset, limit)
                .flatMap(new Func1<TdApi.Messages, Observable<? extends ChatDB.Portion>>() {
                    @Override
                    public Observable<? extends ChatDB.Portion> call(TdApi.Messages portion) {

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
                        log(messageList);
                        for (TdApi.Message message : messageList) {
                            holder.parser.parse(message);
                        }

                        if (tmpUIDs.isEmpty()) {
                            ChatDB.Portion res = new ChatDB.Portion(messageList, Collections.<TdApi.User>emptyList());
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
                            return allUsers.zipWith(messagesCopy, ZIPPER);
                        }
                    }
                })
                .observeOn(mainThread());

        request.subscribe(new ObserverAdapter<ChatDB.Portion>() {
            @Override
            public void onNext(ChatDB.Portion portion) {
                checkMainThread();
                request = null;
                atLeastOneRequestCompleted = true;
                if (portion.ms.isEmpty()) {
                    downloadedAll = true;
                } else {

                    SparseArray<TdApi.User> us = portion.us;
                    holder.saveUsers(us);
                    if (!historyRequest) {
                        ms.clear();
                    }
                    ms.addAll(portion.ms);
                    subject.onNext(getMessages());
                }
            }
        });
    }

    public static void log2(List<ChatListItem> portion, String stage) {
//        Log.d("RxChat", "start " + stage);
//        for (ChatListItem item : portion) {
//            if (item instanceof MessageItem){
//                TdApi.Message msg = ((MessageItem) item).msg;
//                if (msg.message instanceof TdApi.MessageText){
//                    String text = ((TdApi.MessageText) msg.message).text;
//                    Log.d("RxChat", "\t---->>> " + msg.id + " " + text);
//                } else {
//                    Log.d("RxChat", "\t---->>>" + msg.id + " "  + msg.message.getClass().getSimpleName());
//                }
//
//            }
//        }
//        Log.d("RxChat", "end");
    }

    public static void log(List<TdApi.Message> portion) {
//        Log.d("RxChat", "log portion of chat ");
//        for (TdApi.Message message : portion) {
//            if (message.message instanceof TdApi.MessageText){
//                Log.d("RxChat", "\t\t message " + ((TdApi.MessageText) message.message).text);
//            } else {
//                Log.d("RxChat", "\t\t message " + message.id);
//            }
//        }
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
        if (message.message instanceof TdApi.MessageContact){
            TdApi.MessageContact c = (TdApi.MessageContact) message.message;
            tmpUIDs.add(c.userId);
        }
    }

    public Observable<List<TdApi.Message>> messageList() {
        return subject;
    }

    public List<TdApi.Message> getMessages() {
        return new ArrayList<>(ms);
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

    public void updateCurrentMessageList() {
        checkMainThread();
        if (ms.isEmpty()) {
            return;
        }
        if (request == null) {
            int offset = -1;
            List<TdApi.Message> messages = getMessages();
            int size = messages.size();
            requestImpl(messages.get(0), null, false, size, offset);
        } else {

        }
    }



    public void handleNewMessage(TdApi.Message tlObject) {
        ms.add(tlObject);
        newMessage.onNext(tlObject);
    }








    public void deleteHistory() {
        client.sendRx(new TdApi.DeleteChatHistory(id))
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        ms.clear();
                        subject.onNext(getMessages());
                    }
                });
    }

    final SparseArray<TdApi.UpdateMessageId> newIdToUpdate = new SparseArray<>();
    final SparseArray<TdApi.UpdateMessageId> oldIdToUpdate = new SparseArray<>();

    public void updateMessageId(TdApi.UpdateMessageId upd) {
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
                        deleteMessageImpl(messageId);
                        return tlObject;
                    }
                });
    }

    private void deleteMessageImpl(int messageId) {
        for (TdApi.Message message : ms) {
            if (message.id == messageId) {
                ms.remove(message);
                break;
            }
        }

        subject.onNext(getMessages());
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

    public static abstract class ChatListItem {

    }
    public static class MessageItem extends ChatListItem{
        public final TdApi.Message msg;

        public MessageItem(TdApi.Message msg) {
            this.msg = msg;
        }
    }
    public static class DaySeparatorItem extends ChatListItem  {
        public final long id;
        public final DateTime day;//millis

        public DaySeparatorItem(long id, DateTime day) {
            this.id = id;
            this.day = day;
        }
    }

    public int getMessageState(TdApi.Message msg, long lastReadOutbox, int myId){
        if (myId != msg.fromId){
            return MESSAGE_STATE_READ;
        }
        TdApi.UpdateMessageId upd = getUpdForOldId(msg.id);
        if (msg.id >= MSG_WITHOUT_VALID_ID && upd == null){
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
}
