package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.core.rx.RxChat;

public class DocumentVH extends BaseAvatarVH {

    private final DocumentView documentView;

    public DocumentVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        documentView = ((DocumentView) itemView.findViewById(R.id.document_view));
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        TdApi.MessageDocument message = (TdApi.MessageDocument) msg.message;
        documentView.set(message.document);
    }
}
