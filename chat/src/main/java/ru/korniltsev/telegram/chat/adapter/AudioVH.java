package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.core.rx.RxChat;

public class AudioVH extends BaseAvatarVH {

    private final AudioMessageView audioView;

    public AudioVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        audioView = ((AudioMessageView) itemView.findViewById(R.id.audio));
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        super.bind(item);
        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;
        TdApi.MessageAudio msg = (TdApi.MessageAudio) rawMsg.message;
        audioView.setAudio(msg.audio);
    }
}
