package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
        Spannable name = userColor(getNameForSenderOf(message));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(name)
                .append(" ")
                .append(resources.getString(R.string.message_removed_group_photo));
        text.setText(sb);
    }



    private void bindChatAddParticipant(TdApi.Message item) {

        TdApi.MessageChatAddParticipant create = (TdApi.MessageChatAddParticipant) item.message;

//        String inviter = ;
        Spannable inviter = userColor(
                getNameForSenderOf(item));
        Spannable newUser = userColor(
                Utils.uiName(create.user));

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(inviter)
                .append(" ")
                .append(resources.getString(R.string.message_ivited))
                .append(" ")
                .append(newUser);

        text.setText(sb);
    }

    public static Spannable userColor(String str){
        Spannable.Factory factory = Spannable.Factory.getInstance();
        Spannable spannable = factory.newSpannable(str);
        spannable.setSpan(new ForegroundColorSpan(0xff427ab0), 0, spannable.length(), 0);
        spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, spannable.length(), 0);
        return spannable;
    }

    private void bindChatDeleteParticipant(TdApi.Message item) {
        TdApi.MessageChatDeleteParticipant kick = (TdApi.MessageChatDeleteParticipant) item.message;
        Spannable inviter = userColor(getNameForSenderOf(item));
        Spannable newUser = userColor(Utils.uiName(kick.user));

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(inviter)
                .append(" ")
                .append(resources.getString(R.string.message_kicked))
                .append(" ")
                .append(newUser);


        text.setText(sb);
    }

    private void bindChatCreated(TdApi.Message item) {
        TdApi.MessageGroupChatCreate create = (TdApi.MessageGroupChatCreate) item.message;
        Spannable creator = userColor(getNameForSenderOf(item));
        Spannable title = userColor(create.title);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(creator)
                .append(" ")
                .append(resources.getString(R.string.message_created_group))
                .append(" ")
                .append(title);
        text.setText(sb);
    }

    private void bindChatChangedTitle(TdApi.Message item) {
        TdApi.MessageChatChangeTitle create = (TdApi.MessageChatChangeTitle) item.message;
        Spannable creator = userColor(getNameForSenderOf(item));
        Spannable title = userColor(create.title);


        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(creator)
                .append(" ")
                .append(resources.getString(R.string.message_created_group))
                .append(" ")
                .append(title);
        text.setText(sb);
    }

    private void bindUnsupported() {
        text.setText(R.string.message_unsupported);
    }
    private void bindDeleteMessage() {
        text.setText(R.string.message_deleted);
    }


}
