package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RxChat;

//something we can draw as a single textView

//MessageGroupChatCreate extends MessageContent {
//        MessageChatChangeTitle extends MessageContent {
//        MessageChatDeleteParticipant extends MessageContent {
//        MessageChatAddParticipant extends MessageContent {
//        MessageDeleted extends MessageContent {
//        MessageChatDeletePhoto extends MessageContent {
//        MessageUnsupported extends MessageContent {



public class SingleTextViewVH extends RealBaseVH {
    private final TextView text;
    private Resources resources;

    public SingleTextViewVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        text = (TextView) itemView.findViewById(R.id.text);
        resources = itemView.getContext().getResources();
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        TdApi.Message msgRaw = ((RxChat.MessageItem) item).msg;
        TdApi.MessageContent msg = msgRaw.message;
        if (msg instanceof TdApi.MessageChatChangeTitle) {
            bindChatChangedTitle(msgRaw);
        } else if (msg instanceof TdApi.MessageGroupChatCreate) {
            bindChatCreated(msgRaw);
        } else if (msg instanceof TdApi.MessageChatDeleteParticipant) {
            bindChatDeleteParticipant(msgRaw);
        } else if (msg instanceof TdApi.MessageChatAddParticipant) {
            bindChatAddParticipant(msgRaw);
        } else if (msg instanceof TdApi.MessageDeleted){
            bindDeleteMessage();
        }  else if (msg instanceof TdApi.MessageChatDeletePhoto) {
            bindChatDeletePhoto(msgRaw);
        } else {
            bindUnsupported();
        }
    }



    private void bindChatDeletePhoto(TdApi.Message message) {
        String name = getNameForSenderOf(message);
        text.setText(text.getResources().getString(R.string.message_removed_group_photo, name));
    }



    private void bindChatAddParticipant(TdApi.Message item) {
        TdApi.MessageChatAddParticipant create = (TdApi.MessageChatAddParticipant) item.message;
        String inviter = getNameForSenderOf(item);
        String newUser = Utils.uiName(create.user);
        text.setText(
                resources.getString(R.string.message_ivited, inviter, newUser));
    }

    private void bindChatDeleteParticipant(TdApi.Message item) {
        TdApi.MessageChatDeleteParticipant kick = (TdApi.MessageChatDeleteParticipant) item.message;
        String inviter = getNameForSenderOf(item);
        String newUser = Utils.uiName(kick.user);
        text.setText(
                resources.getString(R.string.message_kicked, inviter, newUser));
    }

    private void bindChatCreated(TdApi.Message item) {
        TdApi.MessageGroupChatCreate create = (TdApi.MessageGroupChatCreate) item.message;
        String creator = getNameForSenderOf(item);
        text.setText(
                resources.getString(R.string.message_created_group, creator, create.title));
    }

    private void bindChatChangedTitle(TdApi.Message item) {
        TdApi.MessageChatChangeTitle create = (TdApi.MessageChatChangeTitle) item.message;
        String creator = getNameForSenderOf(item);
        text.setText(
                resources.getString(R.string.message_renamed_group, creator, create.title));
    }

    private void bindUnsupported() {
        text.setText(R.string.message_unsupported);
    }
    private void bindDeleteMessage() {
        text.setText(R.string.message_deleted);
    }


}
