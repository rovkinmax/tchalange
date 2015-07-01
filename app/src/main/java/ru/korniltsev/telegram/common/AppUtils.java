package ru.korniltsev.telegram.common;

import android.content.Context;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;

public class AppUtils {
    public static String uiName(TdApi.User user, Context ctx) {//todo
        if (user == null) {
            return "";
        }
        if ("DELETED".equals(user.phoneNumber)) {
            return ctx.getString(R.string.deleted_account);
        }
        String firstName = user.firstName;
        String lastName = user.lastName;
        String name = uiName(firstName, lastName);
        return name;
    }

    public static String uiName(String firstName, String lastName) {
        String name;
        StringBuilder sb = new StringBuilder();
        if (firstName.length() != 0) {
            sb.append(firstName);
        }
        if (lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        name = sb.toString();
        return name;
    }
}
