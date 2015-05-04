package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;

public class VideoVH extends BaseVH {
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
    public void bind(TdApi.Message item) {
        super.bind(item);
        TdApi.MessageVideo msg = (TdApi.MessageVideo) item.message;
        video.set(msg.video);


    }
}
