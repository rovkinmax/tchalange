package ru.korniltsev.telegram.chat.adapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxChat;

import java.util.Calendar;

public class DaySeparatorVH extends RealBaseVH {
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
        cal.setTimeInMillis(s.time);
        text.setText("Day #" + s.id + " " + cal.getTime());

    }





}
