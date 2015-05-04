package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;

public class AudioVH extends BaseVH {

    private final AudioMessageView audioView;

    public AudioVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        audioView = ((AudioMessageView) itemView.findViewById(R.id.audio));
    }

    @Override
    public void bind(TdApi.Message item) {
        super.bind(item);
        TdApi.MessageAudio msg = (TdApi.MessageAudio) item.message;
        audioView.setAudio(msg.audio);
    }
}
