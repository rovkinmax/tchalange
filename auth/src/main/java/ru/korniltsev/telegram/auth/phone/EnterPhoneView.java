package ru.korniltsev.telegram.auth.phone;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class EnterPhoneView extends LinearLayout {
    private EditText btnSelectCountry;
    private EditText phoneCode;
    private EditText userPhone;
    @Inject EnterPhoneFragment.Presenter presenter;
    @Inject PhoneFormat formatter;
    @Inject Countries countries;
    private Countries.Entry selectedCountry;

    boolean ignorePhoneCodeChanges = false;
    boolean ignorePhoneNumberChanges = false;
    boolean ignorePhoneNumberChange = false;

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
        btnSelectCountry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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
        phoneCode.addTextChangedListener(new TextWatcherAdapter() {

            @Override
            public void afterTextChanged(final Editable s) {
                if (ignorePhoneCodeChanges) {
                    return;
                }
                String code = s.toString();
                if (!code.startsWith("+")) {
                    ignorePhoneCodeChanges = true;
                    s.insert(0, "+");
                    ignorePhoneCodeChanges = false;
                }
                String phonePrefix = s.toString()
                        .replaceAll("\\s+", "");
                final Countries.Entry userTypedCountry = countries.getForPhonePrefix(phonePrefix);
                if (userTypedCountry != null
                        && userTypedCountry != selectedCountry) {
                    countrySelected(userTypedCountry, false);
                }
                updatePhone(userPhone.getText());
            }
        });
        userPhone.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (ignorePhoneNumberChanges){
                    return;
                }

                if (count == 0 && after == 1){
                    //inserted digit
                } else if (count == 1 && after == 0) {
                    //deleted digit
                    ignorePhoneNumberChange = true;
                }

            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (ignorePhoneNumberChange) {
                    ignorePhoneNumberChange = false;
                    return;
                }

                if (ignorePhoneNumberChanges) {
                    return;
                }
                updatePhone(s);

            }
        });
    }

    private void updatePhone(Editable s) {
        ignorePhoneNumberChanges = true;
        String strPhoneCode = textFrom(phoneCode);
        String strPhoneRest = textFrom(userPhone);
        String strip = PhoneFormat.strip(strPhoneCode + strPhoneRest);
        final String restFormatted = formatter.format(strip)
                .substring(strPhoneCode.length())
                .trim();
        s.clear();
        s.insert(0, restFormatted);
        ignorePhoneNumberChanges = false;
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

    public void countrySelected(Countries.Entry c, boolean setPhoneCode) {
        btnSelectCountry.setText(c.name);
        if (setPhoneCode) {
            phoneCode.setText(c.phoneCode);
        }
        selectedCountry = c;
    }

    public EditText getPhoneCode() {
        return phoneCode;
    }

    public void showError(String message) {
        if (message == null) {
            userPhone.setError("error");
        } else {
            if (message.contains("PHONE_NUMBER_INVALID")) {
                userPhone.setError(getResources().getString(R.string.invalid_phone_number));
            } else {
                userPhone.setError(message);
            }
        }
    }
}
