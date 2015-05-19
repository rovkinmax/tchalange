package ru.korniltsev.telegram.auth.code;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.R;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;
import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class EnterCodeView extends RelativeLayout {
    @Inject EnterCode.Presenter presenter;
    private EditText smsCode;

    public EnterCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
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
                                //presenter.sendCode(getPhoneNumber());

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
//        clicks(btnSelectCountry).subscribe(e -> {
//            presenter.selectCountry();
//        });
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
        smsCode.setError(th.getMessage());
        smsCode.requestFocus();
    }
}
