package ru.korniltsev.telegram.auth.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.RelativeLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.auth.R;
import rx.android.view.OnClickEvent;
import rx.functions.Action1;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;
import static rx.android.view.ViewObservable.clicks;

public class EnterPhoneView extends RelativeLayout {
    private EditText btnSelectCountry;
    private EditText phoneCode;
    private EditText userPhone;
    @Inject EnterPhoneFragment.Presenter presenter;

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
                                presenter.sendCode(EnterPhoneView.this.getPhoneNumber());
                            }
                        }
                );
        clicks(btnSelectCountry).subscribe(new Action1<OnClickEvent>() {
            @Override
            public void call(OnClickEvent e) {
                presenter.selectCountry();
            }
        });
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
}
