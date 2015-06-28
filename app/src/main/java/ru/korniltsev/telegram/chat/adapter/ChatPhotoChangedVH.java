package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.photoview.PhotoView;

import static ru.korniltsev.telegram.chat.adapter.SingleTextViewVH.userColor;

public class ChatPhotoChangedVH extends RealBaseVH {

    private final TextView text;
    private final AvatarView image;
    private final Resources res;

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
                        .set(new PhotoView(change.photo));
            }
        });
        res = itemView.getResources();
    }

    @Override
    public void bind(RxChat.ChatListItem item, long lastReadOutbox) {
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        SpannableStringBuilder sb = getTextFor(res, msg, adapter.getUserHolder());
//        String text =  this.text.getResources().getString(R.string.message_changed_group_photo, userName);
        this.text.setText(sb);
        TdApi.MessageChatChangePhoto changed = (TdApi.MessageChatChangePhoto) msg.message;
        TdApi.PhotoSize smallSize = changed.photo.photos[0];
        for (TdApi.PhotoSize photo : changed.photo.photos) {
            if (photo.type.equals("a")){
                smallSize = photo;
            }
        }

        TdApi.Chat o = new TdApi.Chat();

        TdApi.GroupChatInfo groupChatInfo = new TdApi.GroupChatInfo();
        groupChatInfo.groupChat = new TdApi.GroupChat();
        groupChatInfo.groupChat.title = "";//todo !!
        groupChatInfo.groupChat.photoSmall = smallSize.photo;
        o.type = groupChatInfo;
        image.loadAvatarFor(o);

    }

    public static SpannableStringBuilder getTextFor(Resources res, TdApi.Message msg, UserHolder uh) {
        Spannable userName = userColor(sGetNameForSenderOf(uh, msg));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(userName)
                .append(" ")
                .append(res.getString(R.string.message_changed_group_photo));
        return sb;
    }
}
