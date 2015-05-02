package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.views.AvatarView;
import rx.functions.Action1;

public class Adapter extends BaseAdapter<TdApi.Chat, Adapter.VH> {



    private final Action1<TdApi.Chat> clicker;

    public Adapter(Context ctx, Action1<TdApi.Chat> clicker) {
        super(ctx);
        this.clicker = clicker;
        setHasStableIds(true);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = getViewFactory().inflate(R.layout.item_chat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        TdApi.Chat chat = getItem(position);
        TdApi.MessageContent message = chat.topMessage.message;
        if (message instanceof TdApi.MessageText) {
            TdApi.MessageText text = (TdApi.MessageText) message;
            holder.message.setText(text.text);
        } else {
            holder.message.setText(null);
        }
        loadAvatar(holder, chat);
    }

    private void loadAvatar(VH holder, TdApi.Chat chat) {
        holder.avatar.loadAvatarFor(chat);
    }


    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    class VH extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        final TextView message;

        public VH(View itemView) {
            super(itemView);
            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
            message = (TextView) itemView.findViewById(R.id.message);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clicker.call(getItem(getPosition()));
                }
            });


        }
    }


}
