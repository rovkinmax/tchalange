package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import mortar.dagger1support.ObjectGraphService;
import org.telegram.android.DpCalculator;
import org.telegram.android.EmojiPopup;
import org.telegram.android.LayoutObservableLinearLayout;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.EmojiParser;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class MessagePanel extends LinearLayout{

    private ImageView btnLeft;
    private ImageView btnRight;
    private EditText input;

    @Inject ActivityOwner activityOwner;
    @Inject DpCalculator calc;
    @Nullable private EmojiPopup emojiPopup;
    private boolean emojiPopupShowWithKeyboard;
//    private LayoutObservableLinearLayout parent;

    public MessagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
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
        input.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                getParentView().setPadding(0, 0, 0, 0);
            }
        });
        btnLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutObservableLinearLayout parent = getParentView();
                emojiPopup = EmojiPopup.create(activityOwner.expose(), parent, calc);
                emojiPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        emojiPopup = null;
                    }
                });
                assert emojiPopup != null;
                emojiPopupShowWithKeyboard = parent.getKeyboardHeight() > 0;
            }
        });
    }

    private LayoutObservableLinearLayout getParentView() {
        return (LayoutObservableLinearLayout) getParent();
    }

    public boolean isEmojiPopupShown() {
        return emojiPopup != null;
    }

    private void showAttachPopup() {

    }

    OnSendListener listener;

    public void setListener(OnSendListener listener) {
        this.listener = listener;
    }

    public boolean dissmissEmojiPopup() {
        if (emojiPopup == null) {
            return false;
        }
        emojiPopup.dismiss();
        emojiPopup = null;
        Utils.hideKeyboard(input);
        return true;
    }

    public interface OnSendListener {
        void sendText(String text);
    }
}
