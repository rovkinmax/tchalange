package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.util.HashMap;
import java.util.Map;

public class Adapter extends BaseAdapter<TdApi.Message, Adapter.VH> {
    private Map<Integer, TdApi.User> us = new HashMap<>();
    public Adapter(Context ctx) {
        super(ctx);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getViewFactory()
                .inflate(R.layout.item_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        TdApi.Message msg = getItem(position);
        TdApi.MessageContent message = msg.message;
        if (message instanceof TdApi.MessageText) {
            String text = ((TdApi.MessageText) message).text;
            holder.message.setText(text);
        }

        TdApi.User user = us.get(msg.fromId);
        if (user != null) {
            holder.avatar.loadAvatarFor(user);
        } else {
            holder.avatar
                    .picasso2
                    .getPicasso()
                    .cancelRequest(holder.avatar);
        }

    }

    public void add(MessagesHolder.MessagesAndUsers ms) {
        addAll(ms.ms);
        us.putAll(ms.us);
    }

    class VH extends RecyclerView.ViewHolder{
        private final AvatarView avatar;
        private final TextView message;

        public VH(View itemView) {
            super(itemView);
            message = ((TextView) itemView.findViewById(R.id.message));
            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        }
    }
}
