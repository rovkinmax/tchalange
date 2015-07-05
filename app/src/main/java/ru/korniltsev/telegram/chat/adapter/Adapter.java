package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;

// Message types left:

//        MessageContact extends MessageContent {

//
public class Adapter extends BaseAdapter<RxChat.ChatListItem, RealBaseVH> {

    public static final int VIEW_TYPE_PHOTO = 0;
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_STICKER = 2;
    public static final int VIEW_TYPE_AUDIO = 3;
    public static final int VIEW_TYPE_GEO = 4;
    public static final int VIEW_TYPE_VIDEO = 5;
    public static final int VIEW_TYPE_SINGLE_TEXT_VIEW = 6;
    public static final int VIEW_TYPE_CHAT_PHOTO_CHANGED = 7;
    public static final int VIEW_TYPE_DOCUMENT = 8;
    public static final int VIEW_TYPE_DAY_SEPARATOR = 9;
    public static final int VIEW_TYPE_TEXT_FORWARD = 10;
    public static final int VIEW_TYPE_TEXT_FORWARD2 = 11;
    public static final int VIEW_TYPE_GIF = 12;
    public static final int VIEW_TYPE_CONTACT = 13;
    public static final int VIEW_TYPE_NEW_MESSAGES = 14;

//    final Map<Integer, TdApi.User> users = new HashMap<>();
    final RxGlide picasso;
    private long lastReadOutbox;

    RxChat chat;
    public final int myId;

    public Adapter(Context ctx, RxGlide picasso, long lastReadOutbox, int myId) {
        super(ctx);
        this.picasso = picasso;
        this.lastReadOutbox = lastReadOutbox;
        this.myId = myId;
        setHasStableIds(true);
    }

    public void setLastReadOutbox(long lastReadOutbox) {
        this.lastReadOutbox = lastReadOutbox;
        notifyDataSetChanged();//todo
    }

    @Override
    public long getItemId(int position) {
        RxChat.ChatListItem item = getItem(position);
        if (item instanceof RxChat.MessageItem){
            TdApi.Message msg = ((RxChat.MessageItem) item).msg;
            return getIdForMessageItem(msg);
        } else if (item instanceof RxChat.DaySeparatorItem) {
            return ((RxChat.DaySeparatorItem) item).id;
        } else if (item instanceof RxChat.NewMessagesItem){
            return ((RxChat.NewMessagesItem) item).id;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public long getIdForMessageItem(TdApi.Message msg) {
        TdApi.UpdateMessageId upd = chat.getUpdForNewId(msg.id);
        if (upd != null) {
            return upd.oldId;
        }
        return msg.id;
    }

    @Override
    public int getItemViewType(int position) {
        RxChat.ChatListItem item = getItem(position);
        if (item instanceof RxChat.MessageItem){
            RxChat.MessageItem rawMsg = (RxChat.MessageItem) item;
            TdApi.MessageContent message = rawMsg.msg.message;
            if (message instanceof TdApi.MessagePhoto) {
                return VIEW_TYPE_PHOTO;
            } else if (message instanceof TdApi.MessageSticker) {
                return VIEW_TYPE_STICKER;
            } else if (message instanceof TdApi.MessageAudio) {
                return VIEW_TYPE_AUDIO;
            } else if (message instanceof TdApi.MessageGeoPoint) {
                return VIEW_TYPE_GEO;
            } else if (message instanceof TdApi.MessageVideo) {
                return VIEW_TYPE_VIDEO;
            } else if (message instanceof TdApi.MessageText) {
                if (rawMsg.msg.forwardFromId == 0){
                    return VIEW_TYPE_TEXT;
                } else {
                    if (position == getItemCount() -1){
                        return VIEW_TYPE_TEXT_FORWARD;
                    }
                    RxChat.ChatListItem nextItem = getItem(position + 1);
                    if (!(nextItem instanceof RxChat.MessageItem)){
                        return VIEW_TYPE_TEXT_FORWARD;
                    }
                    TdApi.Message nextMessage = ((RxChat.MessageItem) nextItem).msg;
                    if (nextMessage.message instanceof TdApi.MessageText){
                        if (nextMessage.fromId == rawMsg.msg.fromId
                                && nextMessage.forwardFromId != 0
                                && nextMessage.date == rawMsg.msg.date) {
                            return VIEW_TYPE_TEXT_FORWARD2;
                        }
                    }
                    return VIEW_TYPE_TEXT_FORWARD;
                }
            } else if (message instanceof TdApi.MessageChatChangePhoto){
                return VIEW_TYPE_CHAT_PHOTO_CHANGED;
            } else if (message instanceof TdApi.MessageDocument){
                TdApi.Document doc = ((TdApi.MessageDocument) message).document;
                if (doc.mimeType.equals("image/gif")){
                    return VIEW_TYPE_GIF;
                } else {
                    return VIEW_TYPE_DOCUMENT;
                }
            } else if (message instanceof TdApi.MessageContact) {
                return VIEW_TYPE_CONTACT;
            }else{
                return VIEW_TYPE_SINGLE_TEXT_VIEW;
            }
        } else if (item instanceof RxChat.NewMessagesItem) {
            return VIEW_TYPE_NEW_MESSAGES;
        } else {
            return VIEW_TYPE_DAY_SEPARATOR;
        }

    }

    private View inflate(int id, ViewGroup parent) {
        return getViewFactory().inflate(id, parent, false);
    }


    @Override
    public RealBaseVH onCreateViewHolder(ViewGroup p, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_PHOTO: {
                View view = inflate(R.layout.chat_item_photo, p);
                return new PhotoMessageVH(view, this);
            }
            case VIEW_TYPE_STICKER: {
                View view = inflate(R.layout.chat_item_sticker, p);
                return new StickerVH(view, this);
            }
            case VIEW_TYPE_AUDIO: {
                View view = inflate(R.layout.chat_item_audio, p);
                return new AudioVH(view, this);
            }
            case VIEW_TYPE_GEO: {
                View view = inflate(R.layout.chat_item_geo, p);
                return new GeoPointVH(view, this);
            }
            case VIEW_TYPE_VIDEO: {
                View view = inflate(R.layout.chat_item_video, p);
                return new VideoVH(view, this);
            }
            case VIEW_TYPE_TEXT: {
                View view = inflate(R.layout.chat_item_message, p);
                return new TextMessageVH(view, this);
            }
            case VIEW_TYPE_TEXT_FORWARD: {
                View view = inflate(R.layout.chat_item_message_forward, p);
                return new ForwardedTextMessageVH(view, this);
            }
            case VIEW_TYPE_TEXT_FORWARD2: {
                View view = inflate(R.layout.chat_item_message_forward2, p);
                return new ForwardedTextMessage2VH(view, this);
            }
            case VIEW_TYPE_CHAT_PHOTO_CHANGED: {
                View view = inflate(R.layout.chat_item_photo_changed, p);
                return new ChatPhotoChangedVH(view, this);
            }
            case VIEW_TYPE_DOCUMENT: {
                View view = inflate(R.layout.chat_item_document, p);
                return new DocumentVH(view, this);
            }
            case VIEW_TYPE_GIF: {
                View view = inflate(R.layout.chat_item_video, p);
                return new GifDocumentVH(view, this);
            }
            case VIEW_TYPE_CONTACT: {
                View view = inflate(R.layout.chat_item_message_forward, p);
                return new ContactVH(view, this);
            }
            case VIEW_TYPE_DAY_SEPARATOR:{
                View view = inflate(R.layout.chat_item_day_separator, p);
                return new DaySeparatorVH(view, this);
            }
            case VIEW_TYPE_NEW_MESSAGES:{
                View view = inflate(R.layout.chat_item_new_messages, p);
                return new NewMessagesVH(view, this);
            }
            default: {
                View view = inflate(R.layout.chat_item_single_text_view, p);
                return new SingleTextViewVH(view, this);
            }
        }
    }

    @Override
    public void onBindViewHolder(RealBaseVH holder, int position) {
        RxChat.ChatListItem item1 = getItem(position);
//        TdApi.Message item = (TdApi.Message) item1;
        holder.bind(item1, lastReadOutbox);
    }

//    public void addHistory(Portion ms) {
//        addAll(ms.ms);
//        users.putAll(ms.us);
//    }
//
//    public void insertNewMessage(Portion portion) {
//        addFirst(portion.ms);
//        users.putAll(portion.us);
//    }

//    public void updateMessageId(TdApi.UpdateMessageId upd) {
//        List<TdApi.Message> ts = getTs();
//        for (int i = 0; i < ts.size(); i++) {
//            TdApi.Message message = ts.findSmallestBiggerThan(i);
//            if (message.id == upd.oldId) {
//                message.id = upd.newId;
//                notifyItemChanged(i);
//                return;
//            }
//        }
//    }

//    public Map<Integer, TdApi.User> getUsers() {
//        return users;
//    }

    public void setChat(RxChat chat) {
        this.chat = chat;
    }

    public UserHolder getUserHolder() {
        return chat;
    }


    //    public Portion getPortion() {
//        return new Portion(getData(), users);
//    }

//    public static class Portion {
//        public final List<TdApi.Message> ms;
//        public final Map<Integer, TdApi.User> us;
//
//        public Portion(List<TdApi.Message> ms, List<TdApi.User> us) {
//            this.ms = ms;
//            this.us = new HashMap<>();
//            for (TdApi.User u : us) {
//                this.us.put(u.id, u);
//            }
//        }
//
//        public Portion(List<TdApi.Message> ms, Map<Integer, TdApi.User> us) {
//            this.ms = ms;
//            this.us = us;
//        }
//
//        public Portion(TdApi.Message msg) {
//            this.ms = Collections.singletonList(msg);
//            this.us = Collections.emptyMap();
//        }
//    }
}
