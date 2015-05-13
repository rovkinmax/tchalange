package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.core.rx.RxChat;

class PhotoMessageVH extends BaseAvatarVH {
    private final PhotoMessageView image;


    public PhotoMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        image = (PhotoMessageView) itemView.findViewById(R.id.image);
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        super.bind(item);
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        TdApi.MessagePhoto photo = (TdApi.MessagePhoto) msg.message;
        image.load(photo);
    }
}
