package ru.korniltsev.telegram.core.rx;

import android.support.v4.util.LongSparseArray;
import android.text.method.DateTimeKeyListener;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.core.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private int counter = -1;

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
}
