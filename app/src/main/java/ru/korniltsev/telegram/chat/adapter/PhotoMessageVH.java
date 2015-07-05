package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.photoview.PhotoView;

class PhotoMessageVH extends BaseAvatarVH {
    private final PhotoMessageView image;


    public PhotoMessageVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        image = (PhotoMessageView) itemView.findViewById(R.id.image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxChat.MessageItem item = (RxChat.MessageItem) adapter.getItem(getPosition());
                TdApi.MessagePhoto photo = (TdApi.MessagePhoto) item.msg.message;
                Flow.get(v.getContext())
                        .set(new PhotoView(photo.photo, item.msg.id, item.msg.chatId));
            }
        });
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;

        image.load(msg);


    }
}
