package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.photoview.PhotoViewer;

public class ChatPhotoChangedVH extends RealBaseVH {

    private final TextView text;
    private final AvatarView image;

    public ChatPhotoChangedVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        text = ((TextView) itemView.findViewById(R.id.text));
        image = ((AvatarView) itemView.findViewById(R.id.image));
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxChat.MessageItem item = (RxChat.MessageItem) adapter.getItem(getPosition());
                TdApi.MessageChatChangePhoto change = (TdApi.MessageChatChangePhoto) item.msg.message;
                Flow.get(v.getContext())
                        .set(new PhotoViewer(change.photo));
            }
        });
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        String userName = getNameForSenderOf(msg);
        String text =  this.text.getResources().getString(R.string.message_changed_group_photo, userName);
        this.text.setText(text);
        TdApi.MessageChatChangePhoto changed = (TdApi.MessageChatChangePhoto) msg.message;
        TdApi.PhotoSize smallSize = changed.photo.photos[0];
        for (TdApi.PhotoSize photo : changed.photo.photos) {
            if (photo.type.equals("a")){
                smallSize = photo;
            }
        }

        TdApi.Chat o = new TdApi.Chat();
        TdApi.GroupChatInfo groupChatInfo = new TdApi.GroupChatInfo();
        o.type = groupChatInfo;
        groupChatInfo.groupChat = new TdApi.GroupChat();
        groupChatInfo.groupChat.photoSmall = smallSize.photo;
        image.loadAvatarFor(o);

    }
}
