package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;

class TextMessageVH extends BaseAvatarVH {

    private final TextView message;

    public TextMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        message = ((TextView) itemView.findViewById(R.id.message));



    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        super.bind(item);
        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;

        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        message.setText(text.textWithSmilesAndUserRefs);


    }
}
