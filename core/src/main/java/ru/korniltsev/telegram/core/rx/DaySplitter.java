package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.core.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaySplitter {

    public boolean hasTheSameDay(TdApi.Message a, TdApi.Message b) {
        return hasTheSameDay(timInMillis(a), timInMillis(b));
    }

    private long timInMillis(TdApi.Message b) {
        return Utils.dateToMillis(b.date);
    }

    public boolean hasTheSameDay(long aTime, long bTime) {
        DateTime dateTimeA = localTime(aTime);
        DateTime dateTimeB = localTime(bTime);
        return dateTimeA.withTimeAtStartOfDay()
                .equals(dateTimeB.withTimeAtStartOfDay());
    }

    private DateTime localTime(long aTime) {

        DateTimeZone utcZone = DateTimeZone.UTC;
        long localTime = utcZone.convertUTCToLocal(aTime);
        return new DateTime(localTime);
    }

    public List<RxChat.ChatListItem> split(List<TdApi.Message> ms) {
        if (ms.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<RxChat.ChatListItem> res = new ArrayList<>();
        TdApi.Message current = ms.get(0);
        res.add(new RxChat.MessageItem(current));
        for (int i = 1; i < ms.size(); ++i) {
            TdApi.Message it = ms.get(i);
            if (hasTheSameDay(current, it)) {
                res.add(new RxChat.MessageItem(it));
            } else {

                res.add(createSeparator(current));
                res.add(new RxChat.MessageItem(it));
            }
            current = it;
        }
        res.add(createSeparator(current));
        return res;
    }

    private final Map<DateTime, RxChat.DaySeparatorItem> cache = new HashMap<>();
    public static final long ID_NEW_MESSAGES = -1;
    private int counter = -5;

    public RxChat.DaySeparatorItem createSeparator(TdApi.Message msg) {
        DateTime time = localTime(timInMillis(msg))
                .withTimeAtStartOfDay();
        RxChat.DaySeparatorItem cached = cache.get(time);
        if (cached != null) {
            return cached;
        } else {
            RxChat.DaySeparatorItem newSeparator = new RxChat.DaySeparatorItem(counter--, time);
            cache.put(time, newSeparator);
            return newSeparator;
        }
    }

    public List<RxChat.ChatListItem> prepend(List<RxChat.ChatListItem> data, TdApi.Message message) {
        boolean addDateItem = false;
        if (data.isEmpty()) {
            addDateItem = true;
        } else {
            RxChat.MessageItem firstMessage = (RxChat.MessageItem) data.get(0);
            if (!hasTheSameDay(message, firstMessage.msg)) {
                addDateItem = true;
            }
        }
        RxChat.MessageItem newMessageItem = new RxChat.MessageItem(message);
        if (addDateItem) {
            return Arrays.asList(
                    createSeparator(message),
                    newMessageItem
            );
        } else {
            return Collections.<RxChat.ChatListItem>singletonList(
                    newMessageItem
            );
        }
    }

    //notice: may not insert the NewMessagesItem!!
    public RxChat.NewMessagesItem insertNewMessageItem(List<RxChat.ChatListItem> split, TdApi.Chat chat, int myId) {
        int lastReadIndex = -1;
        for (int i = 0, splitSize = split.size(); i < splitSize; i++) {
            RxChat.ChatListItem it = split.get(i);
            if (it instanceof RxChat.MessageItem) {
                final TdApi.Message msg = ((RxChat.MessageItem) it).msg;
                if (msg.id == chat.lastReadInboxMessageId) {
                    lastReadIndex = i;
                }
            }
        }
        final RxChat.NewMessagesItem result = new RxChat.NewMessagesItem(chat.unreadCount, ID_NEW_MESSAGES);
        if (lastReadIndex == -1) {
            split.add(result);
            return result;
        }
        for (int i = lastReadIndex-1; i >= 0; i--) {
            RxChat.ChatListItem it = split.get(i);
            if (it instanceof RxChat.MessageItem){
                final RxChat.MessageItem messageItem = (RxChat.MessageItem) it;
                final TdApi.Message msg = messageItem.msg;
                if (msg.fromId != myId) {//income message
                    int insertIndex = i + 1;
                    if (insertIndex < split.size()//есть чо сверху
                            && split.get(insertIndex) instanceof RxChat.DaySeparatorItem) {//и это сепаратор
                        insertIndex++;
                    }
                    split.add(insertIndex, result);
                    return result;
                }
            }
        }

        return result;
    }
}
