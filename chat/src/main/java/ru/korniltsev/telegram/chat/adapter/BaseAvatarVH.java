package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
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
    public static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("K:mm a");
    private final AvatarView avatar;
    private final TextView nick;
    private final TextView time;



    public BaseAvatarVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        nick = ((TextView) itemView.findViewById(R.id.nick));
        time = (TextView) itemView.findViewById(R.id.time);

        nick.setTextColor(Colors.USER_NAME_COLOR);
        //todo blue dot
        //todo message set status
    }

    public void bind(RxChat.ChatListItem item){
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        TdApi.User user = adapter.getUserHolder().getUser(msg.fromId);
        avatar.loadAvatarFor(user);
        String name = Utils.uiName(user);
        nick.setText(name);
        long timeInMillis = Utils.dateToMillis(msg.date);
        long local = DateTimeZone.UTC.convertUTCToLocal(timeInMillis);
        time.setText(MESSAGE_TIME_FORMAT.print(local));
    }
}
