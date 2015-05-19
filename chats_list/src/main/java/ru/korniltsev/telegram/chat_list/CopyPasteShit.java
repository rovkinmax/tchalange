//package ru.korniltsev.telegram.chat_list;
//
//import android.content.res.Resources;
//import android.text.Spannable;
//import android.text.SpannableStringBuilder;
//import org.drinkless.td.libcore.telegram.TdApi;
//import ru.korniltsev.telegram.chat.adapter.RealBaseVH;
//import ru.korniltsev.telegram.core.Utils;
//import ru.korniltsev.telegram.core.rx.UserHolder;
//
//public class CopyPasteShit {
//    private  static CharSequence getTextFor(Resources res, TdApi.Message msgRaw, TdApi.MessageContent msg, UserHolder uh) {
//        if (msg instanceof TdApi.MessageChatChangeTitle) {
//            TdApi.MessageChatChangeTitle create = (TdApi.MessageChatChangeTitle) msgRaw.message;
//            Spannable creator = userColor(RealBaseVH.sGetNameForSenderOf(uh, msgRaw));
//            Spannable title = userColor(create.title);
//
//            SpannableStringBuilder sb = new SpannableStringBuilder();
//            sb.append(creator)
//                    .append(" ")
//                    .append(res.getString(ru.korniltsev.telegram.chat.R.string.message_created_group))
//                    .append(" ")
//                    .append(title);
//            return sb;
//        } else if (msg instanceof TdApi.MessageGroupChatCreate) {
//            TdApi.MessageGroupChatCreate create = (TdApi.MessageGroupChatCreate) msgRaw.message;
//            Spannable creator = userColor(RealBaseVH.sGetNameForSenderOf(uh, msgRaw));
//            Spannable title = userColor(create.title);
//
//            SpannableStringBuilder sb = new SpannableStringBuilder();
//            sb.append(creator)
//                    .append(" ")
//                    .append(res.getString(ru.korniltsev.telegram.chat.R.string.message_created_group))
//                    .append(" ")
//                    .append(title);
//            return sb;
//            //        text.setText(sb);
//        } else if (msg instanceof TdApi.MessageChatDeleteParticipant) {
//            TdApi.MessageChatDeleteParticipant kick = (TdApi.MessageChatDeleteParticipant) msgRaw.message;
//            Spannable inviter = userColor(RealBaseVH.sGetNameForSenderOf(uh, msgRaw));
//            Spannable newUser = userColor(Utils.uiName(kick.user));
//
//            SpannableStringBuilder sb = new SpannableStringBuilder();
//            sb.append(inviter)
//                    .append(" ")
//                    .append(res.getString(ru.korniltsev.telegram.chat.R.string.message_kicked))
//                    .append(" ")
//                    .append(newUser);
//
//            return sb;
//            //        text.setText(sb);
//        } else if (msg instanceof TdApi.MessageChatAddParticipant) {
//
//            TdApi.MessageChatAddParticipant create = (TdApi.MessageChatAddParticipant) msgRaw.message;
//
//            //        String inviter = ;
//            Spannable inviter = userColor(
//                    RealBaseVH.sGetNameForSenderOf(uh, msgRaw));
//            Spannable newUser = userColor(
//                    Utils.uiName(create.user));
//
//            SpannableStringBuilder sb = new SpannableStringBuilder();
//            sb.append(inviter)
//                    .append(" ")
//                    .append(res.getString(ru.korniltsev.telegram.chat.R.string.message_ivited))
//                    .append(" ")
//                    .append(newUser);
//
//            return sb;
//        } else if (msg instanceof TdApi.MessageDeleted){
//            return res.getString(ru.korniltsev.telegram.chat.R.string.message_deleted);
//        }  else if (msg instanceof TdApi.MessageChatDeletePhoto) {
//            Spannable name = userColor(RealBaseVH.sGetNameForSenderOf(uh, msgRaw));
//            SpannableStringBuilder sb = new SpannableStringBuilder();
//            sb.append(name)
//                    .append(" ")
//                    .append(res.getString(ru.korniltsev.telegram.chat.R.string.message_removed_group_photo));
//            return sb;
//        } else {
//            return res.getString(ru.korniltsev.telegram.chat.R.string.message_unsupported);
//        }
//    }
//
//    private static Spannable userColor(String s) {
//        return null;
//    }
//}
