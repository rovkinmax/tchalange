package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.utils.Colors;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.text.SimpleDateFormat;
import java.util.Locale;

abstract class BaseAvatarVH extends RealBaseVH {
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("K:mm aa")
            .withLocale(Locale.US);
    public static final int MSG_WITHOUT_VALID_ID = 1000000000;
    private final AvatarView avatar;
    private final TextView nick;
    private final TextView time;
    private final ImageView iconRight;
                  private final int myId = adapter.myId;
    public BaseAvatarVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        nick = ((TextView) itemView.findViewById(R.id.nick));
        time = (TextView) itemView.findViewById(R.id.time);
        iconRight = (ImageView) itemView.findViewById(R.id.icon_right);

        colorizeNick(nick);
        nick.setLines(1);
        nick.setSingleLine();
        //todo blue dot
        //todo message set status
    }

    public static void colorizeNick(TextView v) {
        v.setTextColor(Colors.USER_NAME_COLOR_STATE_LIST);
    }

    public void bind(RxChat.ChatListItem item, long lastReadOutbox){
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        TdApi.User user = adapter.getUserHolder().getUser(msg.fromId);
        if (user != null){
            avatar.loadAvatarFor(user);
            String name = Utils.uiName(user);
            nick.setText(name);
        }
        String print = format(msg);
        time.setText(print);
        switch (adapter.chat.getMessageState(msg, lastReadOutbox, myId)){
            case RxChat.MESSAGE_STATE_READ:
                iconRight.setVisibility(View.GONE);
                break;
            case RxChat.MESSAGE_STATE_SENT:
                iconRight.setImageResource(R.drawable.ic_unread);
                iconRight.setVisibility(View.VISIBLE);
                break;
            case RxChat.MESSAGE_STATE_NOT_SENT:
                iconRight.setImageResource(R.drawable.ic_clock);
                iconRight.setVisibility(View.VISIBLE);
                break;
        }
//        TdApi.UpdateMessageId upd = adapter.chat.getUpdForOldId(msg.id);
//        if (msg.id >= MSG_WITHOUT_VALID_ID && upd == null){
//            iconRight.setImageResource(R.drawable.ic_clock);
//            iconRight.setVisibility(View.VISIBLE);
//        } else {
//            //message sent
//            int id = msg.id;
//            if (id >= MSG_WITHOUT_VALID_ID) {
//                id = upd.newId;
//            }
//            if (lastReadOutbox < id){
//                iconRight.setImageResource(R.drawable.ic_unread);
//                iconRight.setVisibility(View.VISIBLE);
//            } else {
//                iconRight.setVisibility(View.GONE);
//            }
//        }

    }

    static SimpleDateFormat fuckRuFormatter = new SimpleDateFormat("kk:mm", Locale.US);
    public static String format(TdApi.Message msg) {
//        Locale l = Locale.getDefault();
//        if (l)
        long timeInMillis = Utils.dateToMillis(msg.date);
        long local = DateTimeZone.UTC.convertUTCToLocal(timeInMillis);
        if (Locale.getDefault().getCountry().equals("RU")){
            return fuckRuFormatter.format(local);
        } else {
            return MESSAGE_TIME_FORMAT.print(local);
        }
    }
}
