package ru.korniltsev.telegram.core;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;

/**
 * Created by korniltsev on 23/04/15.
 */
public class Utils {
    public static String textFrom(EditText e) {
        return e.getText().toString();
    }
    public static void hideKeyboard(EditText e){
        InputMethodManager imm = (InputMethodManager)e.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(e.getWindowToken(), 0);
    }



    @Nullable
    public static TdApi.User getUserFromChat(RXClient client, TdApi.Chat chat){
        TdApi.ChatInfo type = chat.type;
        if (type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) type).user;
            return user;
        } else if (type instanceof TdApi.GroupChatInfo) {//

            int fromId = chat.topMessage.fromId;
            if (fromId != 0) {
                return client.getUser(fromId)
                        .toBlocking()//fuck it we block for user
                        .first();
            } else {
                return null;//todo
            }
        } else {
            Assert.fail();
            return null;
        }

    }

    public static TdApi.File getFileForChat(TdApi.Chat chat) {
        TdApi.ChatInfo type = chat.type;
        if (type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) type).user;
            return user.photoSmall;
        } else if (type instanceof TdApi.GroupChatInfo) {//
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) type).groupChat;
            return groupChat.photoSmall;
        }
        Assert.fail();
        return null;
    }
}
