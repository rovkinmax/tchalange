package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.ImageView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.StickerView;
import ru.korniltsev.telegram.core.rx.RxChat;

class StickerVH extends BaseAvatarVH {
    final StickerView image;
    public StickerVH( View itemView, Adapter adapter) {
        super(itemView, adapter);
        image = (StickerView) itemView.findViewById(R.id.image);
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        super.bind(item);
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        TdApi.MessageSticker sticker = (TdApi.MessageSticker) msg.message;
        image.bind(sticker.sticker);

    }
}
