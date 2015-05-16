package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.views.AvatarView;

class ForwardedTextMessage2VH extends RealBaseVH {

//    private final TextView message;
    private final TextView text;
    private final TextView message_time;
    private final TextView nick;
    private final AvatarView avatar;

    public ForwardedTextMessage2VH(View itemView, Adapter adapter) {
        super(itemView, adapter);
//        message = ((TextView) itemView.findViewById(R.id.message));
        text = ((TextView) itemView.findViewById(R.id.forward_text));
        message_time = ((TextView) itemView.findViewById(R.id.forward_time));
        nick = ((TextView) itemView.findViewById(R.id.forward_nick));
        avatar = ((AvatarView) itemView.findViewById(R.id.forward_avatar));
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
//        super.bind(item);
        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;
        TdApi.MessageContent msg = rawMsg.message;
        TdApi.MessageText text = (TdApi.MessageText) msg;
        this.text.setText(text.textWithSmilesAndUserRefs);



        TdApi.User user = adapter.getUserHolder().getUser(rawMsg.forwardFromId);
        avatar.loadAvatarFor(user);
        nick.setText(
                Utils.uiName(user));
        long forwardDateInMillis = Utils.dateToMillis(rawMsg.forwardDate);
        long localTime = DateTimeZone.UTC.convertUTCToLocal(forwardDateInMillis);
        message_time.setText(BaseAvatarVH.MESSAGE_TIME_FORMAT.print(localTime));



    }
}
