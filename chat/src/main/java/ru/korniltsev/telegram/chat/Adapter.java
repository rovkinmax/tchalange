package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Adapter extends BaseAdapter<TdApi.Message, Adapter.VH> {
    private Map<Integer, TdApi.User> users = new HashMap<>();

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
        } else {
            holder.message.setText("");

        }

        TdApi.User user = users.get(msg.fromId);

        holder.avatar.loadAvatarFor(user);
    }

    public void addHistory(Portion ms) {
        addAll(ms.ms);
        users.putAll(ms.us);
    }

    public void insertNewMessage(Portion portion) {
        addFirst(portion.ms);
        users.putAll(portion.us);
    }

    public void updateMessageId(TdApi.UpdateMessageId upd) {
        List<TdApi.Message> ts = getTs();
        for (int i = 0; i < ts.size(); i++) {
            TdApi.Message message = ts.get(i);
            if (message.id == upd.oldId) {
                message.id = upd.newId;
                notifyItemChanged(i);
                return;
            }
        }

    }

    public Portion getPortion() {
        return new Portion(getData(), users);
    }

    class VH extends RecyclerView.ViewHolder {
        private final AvatarView avatar;
        private final TextView message;

        public VH(View itemView) {
            super(itemView);
            message = ((TextView) itemView.findViewById(R.id.message));
            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
        }
    }

    public static class Portion {
        public final List<TdApi.Message> ms;
        public final Map<Integer, TdApi.User> us;

        public Portion(List<TdApi.Message> ms, List<TdApi.User> us) {
            this.ms = ms;
            this.us = new HashMap<>();
            for (TdApi.User u : us) {
                this.us.put(u.id, u);
            }
        }

        public Portion(List<TdApi.Message> ms, Map<Integer, TdApi.User> us) {
            this.ms = ms;
            this.us = us;
        }

        public Portion(TdApi.Message msg) {
            this.ms = Collections.singletonList(msg);
            this.us = Collections.emptyMap();
        }
    }

    public Map<Integer, TdApi.User> getUsers() {
        return users;
    }
}
