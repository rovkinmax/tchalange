package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;

public class DocumentVH extends BaseAvatarVH {

    private final DocumentView documentView;

    public DocumentVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        documentView = ((DocumentView) itemView.findViewById(R.id.document_view));
    }

    @Override
    public void bind(TdApi.Message item) {
        super.bind(item);
        TdApi.MessageDocument message = (TdApi.MessageDocument) item.message;
        documentView.set(message.document);
    }
}
