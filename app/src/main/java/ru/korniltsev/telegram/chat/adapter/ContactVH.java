package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;

class ContactVH extends BaseAvatarVH {

    //    private final TextView message;
    private final TextView text;
    private final TextView message_time;
    private final TextView nick;
    private final AvatarView avatar;

    public ContactVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        //        message = ((TextView) itemView.findViewById(R.id.message));
        text = ((TextView) itemView.findViewById(R.id.forward_text));
//                todo TextMessageVH.applyTextStyle(text);
        message_time = ((TextView) itemView.findViewById(R.id.forward_time));
        nick = ((TextView) itemView.findViewById(R.id.forward_nick));
        BaseAvatarVH.colorizeNick(nick);
        avatar = ((AvatarView) itemView.findViewById(R.id.forward_avatar));

        message_time.setVisibility(View.GONE);
        text.setTextColor(0xff777777);
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);

        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;
        TdApi.MessageContact msg = (TdApi.MessageContact) rawMsg.message;
//        TdApi.MessageText text = (TdApi.MessageText) msg;
        this.text.setText(msg.phoneNumber);//texm.textWithSmilesAndUserRefs);



        TdApi.User user = adapter.getUserHolder().getUser(msg.userId);
        avatar.loadAvatarFor(user);
        nick.setText(
                AppUtils.uiName(msg.firstName, msg.lastName));
//        long forwardDateInMillis = Utils.dateToMillis(rawMsg.forwardDate);
//        long localTime = DateTimeZone.UTC.convertUTCToLocal(forwardDateInMillis);
//        message_time.setText(BaseAvatarVH.MESSAGE_TIME_FORMAT.print(localTime));



    }
}
