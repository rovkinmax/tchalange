package ru.korniltsev.telegram.common;

import android.content.Context;
import android.content.res.Resources;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.ChatView;
import ru.korniltsev.telegram.chat.R;

public class AppUtils {
    private static DateTimeFormatter SUBTITLE_FORMATTER = DateTimeFormat.forPattern("dd/MM/yy");

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

    public static String uiUserStatus(Context context, TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) {
            return context.getString(R.string.user_status_online);
        } else if (status instanceof TdApi.UserStatusOffline) {
            long wasOnline = ((TdApi.UserStatusOffline) status).wasOnline;
            long timeInMillis = wasOnline * 1000;
            //            Date date = new Date(timeInMillis);
            DateTime wasOnlineTime = new DateTime(timeInMillis, DateTimeZone.UTC)
                    .withZone(DateTimeZone.getDefault());

            DateTime now = DateTime.now();


            String offlineStatusText;
            int daysBetween = Days.daysBetween(wasOnlineTime, now)
                    .getDays();
            final Resources res = context.getResources();
            if (daysBetween == 0) {
                int hoursBetween = Hours.hoursBetween(wasOnlineTime, now)
                        .getHours();
                if (hoursBetween == 0) {
                    int minutesBetween = Minutes.minutesBetween(wasOnlineTime, now)
                            .getMinutes();
                    if (minutesBetween == 0) {
                        //just now
                        offlineStatusText = res.getString(R.string.user_status_just_now);
                    } else if (minutesBetween > 0) {
                        //n minutes
                        offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_minutes_ago, minutesBetween, minutesBetween);
                    } else {
                        //user has wrong date - fallback to SUBTITLE_FORMATTER
                        String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                        offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                    }
                } else if (hoursBetween > 0){
                    //show hours
                    offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_hours_ago, hoursBetween, hoursBetween);
                } else {
                    //user has wrong date - fallback to SUBTITLE_FORMATTER
                    String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                    offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                }
            } else if (daysBetween > 0){
                //show n days ago
                if (daysBetween <= 7){
                    offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_days_ago, daysBetween, daysBetween);
                } else {
                    String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                    offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                }
            } else {
                //user has wrong date - fallback to SUBTITLE_FORMATTER
                String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                offlineStatusText = res.getString(R.string.user_status_last_seen, date);
            }

            return  offlineStatusText;
        } else if (status instanceof TdApi.UserStatusLastWeek) {
            return context.getString(R.string.user_status_last_week);
        } else if (status instanceof TdApi.UserStatusLastMonth) {
            return context.getString(R.string.user_status_last_month);
        } else if (status instanceof TdApi.UserStatusRecently) {
            return context.getString(R.string.user_status_recently);
        } else {
            //empty
            return "";
        }
    }
}
