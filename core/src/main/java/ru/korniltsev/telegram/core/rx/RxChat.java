package ru.korniltsev.telegram.core.rx;

import android.util.SparseArray;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

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

    public final Func2<List<TdApi.User>, List<TdApi.Message>, ChatDB.Portion> ZIPPER = new Func2<List<TdApi.User>, List<TdApi.Message>, ChatDB.Portion>() {
        @Override
        public ChatDB.Portion call(List<TdApi.User> users, List<TdApi.Message> messages) {
            return new ChatDB.Portion(messages, users, splitter.split(messages));
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
    private Action1<TdApi.Message> HANDLE_NEW_MESSAGE = new Action1<TdApi.Message>() {
        @Override
        public void call(TdApi.Message tlObject) {
            handleNewMessage(tlObject);
        }
    };


    final long id;
    final RXClient client;
    public final ChatDB holder;

    private final List<ChatListItem> chatListItems = new ArrayList<>();
    private final List<TdApi.Message> messages = new ArrayList<>();

    private final PublishSubject<List<ChatListItem>> subject = PublishSubject.create();
    private final DaySplitter splitter;
    private Observable<ChatDB.Portion> request;
    private Set<Integer> tmpUIDs = new HashSet<>();

    private boolean atLeastOneRequestCompleted = false;
    private boolean downloadedAll;


    public RxChat(long id, RXClient client, ChatDB holder) {
        this.id = id;
        this.client = client;
        this.holder = holder;

        splitter = new DaySplitter();
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
        TdApi.Message lastMessage = messages.get(messages.size() - 1);
        requestImpl(lastMessage, null, true, holder.getMessageLimit(), 0);
    }

    private void requestImpl(TdApi.Message lastMessage, final TdApi.Message initMessage, final boolean historyRequest, int limit, int offset) {
        assertNull(request);
        //        Log.e("RxChat", "request", new Throwable());
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
                        for (TdApi.Message message : messageList) {
                            holder.parser.parse(message);
                        }

                        if (tmpUIDs.isEmpty()) {
                            ChatDB.Portion res = new ChatDB.Portion(messageList, Collections.<TdApi.User>emptyList(), splitter.split(messageList));
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

        request.subscribe(new Action1<ChatDB.Portion>() {
            @Override
            public void call(ChatDB.Portion portion) {
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
                        chatListItems.clear();
                    }
                    messages.addAll(portion.ms);
                    splitter.append(portion.items, chatListItems);
                    subject.onNext(chatListItems);
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

    public Observable<List<ChatListItem>> messageList() {
        return subject;
    }

    public List<ChatListItem> getMessages() {
        return chatListItems;
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
        if (messages.isEmpty()) {
            return;
        }
        if (request == null) {
            int offset = -1;
            int size = messages.size();
            requestImpl(messages.get(0), null, false, size, offset);
        } else {

        }
    }



    public void handleNewMessage(TdApi.Message tlObject) {
        messages.add(0, tlObject);

        List<ChatListItem> prepend = splitter.prepend(tlObject, chatListItems);
        newMessage.onNext(prepend);
    }

    private PublishSubject<List<ChatListItem>> newMessage = PublishSubject.create();
    public Observable<List<ChatListItem>> newMessage() {
        return newMessage;
    }

    public void deleteHistory() {
        client.sendRx(new TdApi.DeleteChatHistory(id))
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject o) {
                        messages.clear();
                        chatListItems.clear();
                        subject.onNext(chatListItems);
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
        for (TdApi.Message message : messages) {
            if (message.id == messageId) {
                messages.remove(message);
                break;
            }
        }

        for (int i = 0; i < chatListItems.size(); i++) {
            ChatListItem chatListItem = chatListItems.get(i);
            if (chatListItem instanceof MessageItem) {
                TdApi.Message msg = ((MessageItem) chatListItem).msg;
                if (msg.id == messageId) {
                    chatListItems.remove(i);
                    break;//todo delete day separator too
                }
            }
        }
        subject.onNext(chatListItems);
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

    public void hackToReadTheMessage(List<ChatListItem> chatListItems) {
        MessageItem msg = (MessageItem) chatListItems.get(0);
        client.sendRx(new TdApi.GetChatHistory(id, msg.msg.id, -1, 1))
        .subscribe(new Action1<TdApi.TLObject>() {
            @Override
            public void call(TdApi.TLObject tlObject) {

            }
        });

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
//            iconRight.setImageResource(R.drawable.ic_clock);
//            iconRight.setVisibility(View.VISIBLE);
        } else {
            //message sent
            int id = msg.id;
            if (id >= MSG_WITHOUT_VALID_ID) {
                id = upd.newId;
            }
            if (lastReadOutbox < id) {
                return MESSAGE_STATE_SENT;
//                iconRight.setImageResource(R.drawable.ic_unread);
//                iconRight.setVisibility(View.VISIBLE);
            } else {
                return MESSAGE_STATE_READ;
//                iconRight.setVisibility(View.GONE);
            }
        }
    }
}
