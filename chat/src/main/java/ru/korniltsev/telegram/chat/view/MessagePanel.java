package ru.korniltsev.telegram.chat.view;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class MessagePanel extends LinearLayout{

    private ImageView btnLeft;
    private ImageView btnRight;
    private EditText input;

    public MessagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        btnLeft = (ImageView)findViewById(R.id.btn_left);
        btnRight = (ImageView)findViewById(R.id.btn_right);
        input = (EditText)findViewById(R.id.input);
        input.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0){
                    btnRight.setImageResource(R.drawable.ic_attach);
                } else {
                    btnRight.setImageResource(R.drawable.ic_send);
                }
            }
        });
        btnRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = textFrom(input);
                if (text.length() == 0) {
                    showAttachPopup();
                } else {
                    listener.sendText(
                            text);
                    input.setText("");
                }
            }
        });
    }

    private void showAttachPopup() {

    }

    OnSendListener listener;

    public void setListener(OnSendListener listener) {
        this.listener = listener;
    }

    public interface OnSendListener {
        void sendText(String text);
    }
}
