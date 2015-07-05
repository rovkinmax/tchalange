package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;

public class NewMessagesVH extends RealBaseVH {

    private final TextView text;
    private final Resources res;

    public NewMessagesVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        text = ((TextView) itemView.findViewById(R.id.text));
        res = itemView.getResources();
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        final RxChat.NewMessagesItem i = (RxChat.NewMessagesItem) item;
        final String label = res.getQuantityString(R.plurals.n_new_messages, i.newMessagesCount, i.newMessagesCount);
        this.text.setText(label);
    }
}
