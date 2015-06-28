package ru.korniltsev.telegram.chat.adapter;

import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.utils.Colors;

class TextMessageVH extends BaseAvatarVH {

    private final TextView message;

    public TextMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        message = ((TextView) itemView.findViewById(R.id.message));
        applyTextStyle(message);


    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;

        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        message.setText(text.textWithSmilesAndUserRefs);


    }

    public static void applyTextStyle(TextView text) {
        text.setAutoLinkMask(Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);
        text.setLinkTextColor(Colors.USER_NAME_COLOR);
    }
}
