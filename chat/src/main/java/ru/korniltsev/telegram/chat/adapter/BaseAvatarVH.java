package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.text.SimpleDateFormat;
import java.util.Locale;

abstract class BaseAvatarVH extends RealBaseVH {
    private final SimpleDateFormat MESSAGE_TIME_FORMAT = new SimpleDateFormat("K:mm a", Locale.getDefault());
    private final AvatarView avatar;
    private final TextView nick;
    private final TextView time;



    public BaseAvatarVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        nick = ((TextView) itemView.findViewById(R.id.nick));
        time = (TextView) itemView.findViewById(R.id.time);

        //todo blue dot
        //todo message set status
    }

    public void bind(TdApi.Message item){
        TdApi.User user = adapter.users.get(item.fromId);
        avatar.loadAvatarFor(user);
        String name = Utils.uiName(user);
        nick.setText(name);
        long timeInMillis = item.date * 1000;
        time.setText(MESSAGE_TIME_FORMAT.format(timeInMillis));
    }
}
