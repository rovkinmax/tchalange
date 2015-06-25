package ru.korniltsev.telegram.auth.code;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;

import javax.inject.Inject;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class EnterCodeView extends LinearLayout {
    public static final String PHONE_CODE_INVALID = "PHONE_CODE_INVALID";
    private final String errorMessageUnknown;
    private final String errorMessageInvalidCode;
    @Inject EnterCode.Presenter presenter;
    private EditText smsCode;

    public EnterCodeView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        ObjectGraphService.inject(ctx, this);
        errorMessageUnknown = ctx.getString(R.string.unknown_error);
        errorMessageInvalidCode = ctx.getString(R.string.invalid_code);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initToolbar(this)
                .setTitle(R.string.phone_number)
                .addMenuItem(
                        R.menu.send_code,
                        R.id.menu_send_code,
                        new Runnable() {
                            @Override
                            public void run() {
                                sendCode();
                            }
                        }
                );
        smsCode = ((EditText) findViewById(R.id.sms_code));
        TextView weHaveSentCode = ((TextView) findViewById(R.id.we_have_sent));
        String string = getResources().getString(R.string.we_ve_sent, presenter.getPath().phoneNumber);
        weHaveSentCode.setText(string);
        smsCode.requestFocus();
        smsCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendCode();
                    return true;
                }
                return false;
            }
        });
        smsCode.addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                presenter.codeEntered(s);
            }
        });
    }

    private void sendCode() {
        presenter.checkCode(textFrom(smsCode));
    }

    public EditText getSmsCode() {
        return smsCode;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void showError(Throwable th) {
        smsCode.setError(gerErrorMessageForException(th));
        smsCode.requestFocus();
    }

    private String gerErrorMessageForException(Throwable th) {
        String message = th.getMessage();
        if (isEmpty(message)) {
            return errorMessageUnknown;
        } else if (message.contains(PHONE_CODE_INVALID)) {
            return errorMessageInvalidCode;
        } else {
            return message;
        }
    }
}
