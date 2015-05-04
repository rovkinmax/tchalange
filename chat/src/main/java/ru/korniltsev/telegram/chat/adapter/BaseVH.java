package ru.korniltsev.telegram.chat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.text.SimpleDateFormat;
import java.util.Locale;

abstract class BaseVH extends RecyclerView.ViewHolder{
    private final SimpleDateFormat MESSAGE_TIME_FORMAT = new SimpleDateFormat("K:mm a", Locale.getDefault());
    private final AvatarView avatar;
    private final TextView nick;
    private final TextView time;

    private final Adapter adapter;

    public BaseVH(View itemView, Adapter adapter) {
        super(itemView);
        this.adapter = adapter;
        avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        nick = ((TextView) itemView.findViewById(R.id.nick));
        time = (TextView) itemView.findViewById(R.id.time);

        //todo blue dot
        //todo message set status
    }

    public void bind(TdApi.Message item){
        TdApi.User user = adapter.users.get(item.fromId);
        avatar.loadAvatarFor(user);
        String name = name(user);
        nick.setText(name);
        long timeInMillis = item.date * 1000;
        time.setText(MESSAGE_TIME_FORMAT.format(timeInMillis));
    }

    private String name(TdApi.User user) {//todo
        String name;
        StringBuilder sb = new StringBuilder();
        if (user.firstName.length() != 0) {
            sb.append(user.firstName);
        }
        if (user.lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(user.lastName);
        }
        name = sb.toString();
        return name;
    }
}
