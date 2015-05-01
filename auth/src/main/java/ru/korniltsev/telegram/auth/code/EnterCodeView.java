package ru.korniltsev.telegram.auth.code;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
                                hideKeyboard(smsCode);
                                presenter.checkCode(textFrom(smsCode));
                            }
                        }
                );
        smsCode = ((EditText) findViewById(R.id.sms_code));
//        clicks(btnSelectCountry).subscribe(e -> {
//            presenter.selectCountry();
//        });
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


}
