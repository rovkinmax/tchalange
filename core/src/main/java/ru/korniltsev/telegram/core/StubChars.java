package ru.korniltsev.telegram.core;

import org.drinkless.td.libcore.telegram.TdApi;

public class StubChars {
    public static String stub(TdApi.User user) {
        StringBuilder sb = new StringBuilder();
        if (user.firstName.length() >0) {
            sb.append(user.firstName.charAt(0));
        }
        if (user.lastName.length() >0) {
            sb.append(user.lastName.charAt(0));
        }
        return sb.toString();
    }

    public static String stub(TdApi.GroupChat groupChat) {
        if (groupChat.title.length() > 0) {
            return String.valueOf(
                    Character.toUpperCase(groupChat.title.charAt(0)));
        }
        return "";
    }
}
