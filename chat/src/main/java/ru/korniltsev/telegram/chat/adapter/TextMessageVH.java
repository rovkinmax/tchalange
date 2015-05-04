package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;

class TextMessageVH extends BaseVH {

    private final TextView message;

    public TextMessageVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        message = ((TextView) itemView.findViewById(R.id.message));



    }

    @Override
    public void bind(TdApi.Message item) {
        super.bind(item);
        TdApi.MessageContent msg = item.message;
        if (msg instanceof TdApi.MessageText) {//todo get rid of this "if"
            String text = ((TdApi.MessageText) msg).text;
            message.setText(text);
        } else {
            message.setText("");
        }


    }
}
