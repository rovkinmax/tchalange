package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DaySeparatorVH extends RealBaseVH {
    public static SimpleDateFormat FORMATTER;

    static {

        Locale l = Locale.getDefault();
        if (l.getCountry().equals("RU")){
            FORMATTER = new SimpleDateFormat("d MMMM", l);
        } else {
            FORMATTER = new SimpleDateFormat("MMMM d", l);
        }
    }

    private final TextView text;
    private final Calendar cal;

    public DaySeparatorVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        text = (TextView) itemView.findViewById(R.id.text);
        cal = Calendar.getInstance();
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        RxChat.DaySeparatorItem s = (RxChat.DaySeparatorItem) item;
//        cal.setTimeInMillis(s.time);
        text.setText(FORMATTER.format(s.time));//"Day #" + s.id + " " + cal.getTime());

    }





}
