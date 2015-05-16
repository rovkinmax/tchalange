package ru.korniltsev.telegram.core.rx;

import android.support.v4.util.LongSparseArray;
import android.text.method.DateTimeKeyListener;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.core.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaySplitter {
//    private static final ThreadLocal<Calendar> threadLocalA = new ThreadLocal<Calendar>() {
//        @Override
//        protected Calendar initialValue() {
//            return Calendar.getInstance();
//        }
//    };

//    private static final ThreadLocal<Calendar> threadLocalB = new ThreadLocal<Calendar>() {
//        @Override
//        protected Calendar initialValue() {
//            return Calendar.getInstance();
//        }
//    };

    public boolean hasTheSameDay(TdApi.Message a, TdApi.Message b) {
        return hasTheSameDay(timInMillis(a), timInMillis(b));
    }

    private long timInMillis(TdApi.Message b) {
        return Utils.dateToMillis(b.date);
    }

    public boolean hasTheSameDay(long aTime, long bTime) {
        //        new DateTimeKeyListener()
        DateTime dateTimeA = localTime(aTime);
        DateTime dateTimeB = localTime(bTime);

        return dateTimeA.withTimeAtStartOfDay().equals(dateTimeB.withTimeAtStartOfDay());

        //        dateTimeA.day
//        Calendar calA = threadLocalA.get();
//        Calendar calB = threadLocalB.get();
//        calA.setTimeInMillis(aTime);
//        calB.setTimeInMillis(bTime);
//        round(calA);
//        round(calB);
//        return calA.getTimeInMillis() == calB.getTimeInMillis();
    }

    private DateTime localTime(long aTime) {

        DateTimeZone utcZone = DateTimeZone.UTC;
        long localTime = utcZone.convertUTCToLocal(aTime);
        return new DateTime(localTime);
    }

    public  List<RxChat.ChatListItem> split(List<TdApi.Message> ms) {
        if (ms.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<RxChat.ChatListItem> res = new ArrayList<>();
        TdApi.Message current = ms.get(0);
        res.add(new RxChat.MessageItem(current));
        for (int i = 1; i < ms.size() ; ++i) {
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

    //guarded by cache
    private final Map<DateTime, RxChat.DaySeparatorItem> cache = new HashMap<>();
    //guarded by cache
    private int counter = -1;

    public RxChat.DaySeparatorItem createSeparator(TdApi.Message msg) {
        DateTime time = localTime(timInMillis(msg))
                .withTimeAtStartOfDay();
        synchronized (cache) {
            RxChat.DaySeparatorItem cached = cache.get(time);
            if (cached != null) {
                return cached;
            } else {
                RxChat.DaySeparatorItem newSeparator = new RxChat.DaySeparatorItem(counter--, time);
                cache.put(time, newSeparator);
                return newSeparator;
            }
        }
    }


    public void append(List<RxChat.ChatListItem> what, List<RxChat.ChatListItem> into) {
        if (what.isEmpty()) {
            return ;
        }
        if (into.isEmpty()) {
            into.addAll(what);
            return ;
        }
        RxChat.MessageItem first = (RxChat.MessageItem) what.get(0);
        RxChat.MessageItem last = (RxChat.MessageItem) into.get(into.size() - 2);
        if (hasTheSameDay(first.msg, last.msg)){
            RxChat.DaySeparatorItem remove = (RxChat.DaySeparatorItem) into.remove(into.size() - 1);
        }
        into.addAll(what);
    }

    public List<RxChat.ChatListItem> prepend(TdApi.Message msg, List<RxChat.ChatListItem> ms){
        List<RxChat.ChatListItem> newItems = new ArrayList<>(2);
        RxChat.MessageItem object = new RxChat.MessageItem(msg);
        newItems.add(object);
        if (ms.isEmpty()) {
            RxChat.DaySeparatorItem sep = createSeparator(msg);
            newItems.add(sep);
            ms.add(0, sep);
            ms.add(0, object);
            return newItems;
        }
        RxChat.MessageItem messageItem = (RxChat.MessageItem) ms.get(0);
        if (hasTheSameDay(msg, messageItem.msg)) {
            ms.add(0,object);
        } else {
            RxChat.DaySeparatorItem sep = createSeparator(msg);
            newItems.add(sep);
            ms.add(0, sep);
            ms.add(0, object);
        }
        return newItems;
    }
}
