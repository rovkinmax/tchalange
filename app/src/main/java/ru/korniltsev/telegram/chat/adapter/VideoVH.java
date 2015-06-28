package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.core.rx.RxChat;

public class VideoVH extends BaseAvatarVH {
    private final VideoView video;


    public VideoVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        video = ((VideoView) itemView.findViewById(R.id.video));

//        videoParent.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TdApi.Message item = adapter.getItem(getPosition());
////                Intent intent = new Intent(Intent.ACTION_VIEW);
////                intent.setDataAndType(Uri.fromFile(f), "video/mp4");
////                getParentActivity().startActivityForResult(intent, 500);
//            }
//        });
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message rawMsg = ((RxChat.MessageItem) item).msg;

        TdApi.MessageVideo msg = (TdApi.MessageVideo) rawMsg .message;
        video.set(msg.video);


    }
}
