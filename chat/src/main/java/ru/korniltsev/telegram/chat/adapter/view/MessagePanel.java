package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;
import flow.path.Path;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.emoji.EmojiKeyboardView;
import ru.korniltsev.telegram.core.emoji.EmojiPopup;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RxChat;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class MessagePanel extends LinearLayout {
    public static final int LEVEL_SEND = 0;
    public static final int LEVEL_ATTACH = 1;

    public static final int LEVEL_SMILE = 0;
    public static final int LEVEL_KB = 1;
    public static final int LEVEL_ARROW = 2;
    private final int dip1;
    private ImageView btnLeft;
    private ImageView btnRight;
    private EditText input;

    @Inject ActivityOwner activityOwner;
    @Inject Emoji emoji;
    @Inject DpCalculator calc;
    @Inject ChatDB chat;
    @Nullable private EmojiPopup emojiPopup;
    private boolean emojiPopupShowWithKeyboard;
    private EmojiKeyboardView.CallBack emojiKeyboardCallback = new EmojiKeyboardView.CallBack() {
        @Override
        public void backspaceClicked() {
            input.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
        }

        @Override
        public void emojiClicked(long code) {
            String strEmoji = emoji.toString(code);
            Editable text = input.getText();
            text.append(emoji.replaceEmoji(strEmoji));
        }

        @Override
        public void stickerCLicked(String stickerFilePath) {
            Chat c = Chat.get(getContext());
            RxChat rxChat = chat.getRxChat(c.chat.id);
            rxChat.sendSticker(stickerFilePath);
        }
    };

    public MessagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);

        dip1 = calc.dp(1);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        btnLeft = (ImageView) findViewById(R.id.btn_left);
        btnLeft.setImageLevel(LEVEL_SMILE);
        btnRight = (ImageView) findViewById(R.id.btn_right);
        btnRight.setImageLevel(LEVEL_ATTACH);
        input = (EditText) findViewById(R.id.input);
        input.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    btnRight.setImageLevel(LEVEL_ATTACH);
                } else {
                    btnRight.setImageLevel(LEVEL_SEND);
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
                if (emojiPopup != null) {
                    emojiPopup.dismiss();
                } else {
                    ObservableLinearLayout parent = getParentView();
                    emojiPopup = EmojiPopup.create(activityOwner.expose(), parent, emojiKeyboardCallback);
                    emojiPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            emojiPopup = null;
                            btnLeft.setImageLevel(LEVEL_SMILE);
                        }
                    });
                    assert emojiPopup != null;
                    emojiPopupShowWithKeyboard = parent.getKeyboardHeight() > 0;
                    if (emojiPopupShowWithKeyboard) {
                        btnLeft.setImageLevel(LEVEL_KB);
                    } else {
                        btnLeft.setImageLevel(LEVEL_ARROW);
                    }
                }
            }
        });
    }

    private ObservableLinearLayout getParentView() {
        return (ObservableLinearLayout) getParent();
    }

    //    public boolean isEmojiPopupShown() {
    //        return emojiPopup != null;
    //    }

    private void showAttachPopup() {
        Toast.makeText(getContext(), "Unimplemented ;(", Toast.LENGTH_LONG).show();
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

    final Paint p = new Paint();

    {
        p.setColor(0xffd0d0d0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), dip1, p);
    }
}
