package ru.korniltsev.telegram.chat;

import android.support.annotation.Nullable;
import org.drinkless.td.libcore.telegram.TdApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//todo delete it i dont like it
public class MessagesHolder {
    private final List<TdApi.Message> ms = new ArrayList<>();
    private final Map<Integer, TdApi.User> us = new HashMap<>();

    public void add(TdApi.Message topMessage, @Nullable TdApi.User u) {
        ms.add(topMessage);
        if (u != null){
            us.put(u.id, u);
        }
    }

    public boolean isEmpty() {
        return ms.isEmpty();
    }

    public MessagesAndUsers getMs() {
        return new MessagesAndUsers(ms, us);
    }

    public void add(MessagesAndUsers messages) {
        for (TdApi.Message message : messages.ms) {
            ms.add(message);
        }
        this.us.putAll(messages.us);
    }

    public TdApi.Message getLastMessage() {
        return ms.get(ms.size() - 1);
    }


    //todo delete this shit ASAP
    public static class MessagesAndUsers {
        final List<TdApi.Message> ms;
        final Map<Integer, TdApi.User> us ;


        public MessagesAndUsers(List<TdApi.Message> ms, List<TdApi.User> us) {
            this.ms = ms;
            this.us = new HashMap<>();
            for (TdApi.User u : us) {
                this.us.put(u.id, u);
            }
        }

        public MessagesAndUsers(List<TdApi.Message> ms, Map<Integer, TdApi.User> us) {
            this.ms = ms;
            this.us = us;
        }
    }
}
