package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;

class PhotoMessageVH extends BaseAvatarVH {
    private final PhotoMessageView image;


    public PhotoMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        image = (PhotoMessageView) itemView.findViewById(R.id.image);
    }

    @Override
    public void bind(TdApi.Message item) {
        super.bind(item);

        TdApi.MessagePhoto photo = (TdApi.MessagePhoto) item.message;
        image.load(photo);
    }
}
