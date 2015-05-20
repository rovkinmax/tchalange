package ru.korniltsev.telegram.auth.phone;

import android.content.Context;
import android.graphics.ColorFilter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import rx.android.view.OnClickEvent;
import rx.functions.Action1;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;
import static rx.android.view.ViewObservable.clicks;

public class EnterPhoneView extends LinearLayout {
    private EditText btnSelectCountry;
    private EditText phoneCode;
    private EditText userPhone;
    @Inject EnterPhoneFragment.Presenter presenter;
//    @Inject ActivityOwner owner;

    public EnterPhoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        btnSelectCountry = (EditText) findViewById(R.id.btn_select_country);
        phoneCode = (EditText) findViewById(R.id.country_phone_code);//todo editable
        userPhone = (EditText) findViewById(R.id.user_phone);
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
        clicks(btnSelectCountry).subscribe(new Action1<OnClickEvent>() {
            @Override
            public void call(OnClickEvent e) {
                presenter.selectCountry();
            }
        });

        userPhone.requestFocus();
        userPhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendCode();
                    return true;
                }
                return false;
            }
        });
    }

    private void sendCode() {
        presenter.sendCode(EnterPhoneView.this.getPhoneNumber());
    }

    private String getPhoneNumber() {
        return textFrom(phoneCode) + textFrom(userPhone);
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

    public void countrySelected(Countries.Entry c) {
        btnSelectCountry.setText(c.name);
        phoneCode.setText(c.phoneCode);
    }

    public EditText getPhoneCode() {
        return phoneCode;
    }

    public void showError(String message) {
        userPhone.setError(message);
    }
}
