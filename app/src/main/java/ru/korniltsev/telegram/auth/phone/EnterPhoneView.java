package ru.korniltsev.telegram.auth.phone;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;

import javax.inject.Inject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    boolean ignorePhoneCodeChanges = true;
    boolean ignorePhoneNumberChanges = true;

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
                        R.menu.auth_send_code,
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
        phoneCode.addTextChangedListener(
                new TextWatcherAdapter() {
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
                        countrySelected(userTypedCountry, false);
                        if (userTypedCountry != null) {
                            formatPhone(userPhone.getText(), s);
                            userPhone.requestFocus();
                        }
                    }
                }

        );
        userPhone.addTextChangedListener(
                new TextWatcherAdapter() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        if (ignorePhoneNumberChanges) {
                            return;
                        }

                        if (count == 0 && after == 1) {
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
                        if (selectedCountry == null){
                            return;
                        }
                        formatPhone(s, phoneCode.getText());
                    }
                }

        );
    }

    private void formatPhone(Editable phone, Editable code) {
        ignorePhoneNumberChanges = true;
        String strPhoneCode = code.toString();
        String strPhoneRest = phone.toString();//textFrom(userPhone);
        String strip = PhoneFormat.strip(strPhoneCode + strPhoneRest);
        String formatted = formatter.format(strip);
        final String restFormatted = formatted
                .substring(Math.min(strPhoneCode.length(), formatted.length()))
                .trim();
        phone.clear();
        phone.insert(0, restFormatted);
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

    public void countrySelected(@Nullable Countries.Entry c, boolean setPhoneCode) {
        if (c == null){
            btnSelectCountry.setText(R.string.choose_your_country);
            if (setPhoneCode){
                phoneCode.setText("+");
            }
            selectedCountry = null;
        } else {
            btnSelectCountry.setText(c.localizedName());
            if (setPhoneCode) {
                phoneCode.setText(c.phoneCode);
            }
            selectedCountry = c;
        }
        ignorePhoneNumberChanges = false;
        ignorePhoneCodeChanges = false;

    }

    public Countries.Entry getSelectedCountry() {
        return selectedCountry;
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
                //                FLOOD_WAIT_19394 AuthSetPhoneNumber {
                //                    phoneNumber = +7(911)
                //                }
                Pattern floodPattern = Pattern.compile("FLOOD_WAIT_(\\d+).*");
                Matcher match = floodPattern.matcher(message.replaceAll("\n", ""));
                if (match.matches()) {
                    int secondsToWait = Integer.parseInt(match.group(1));
                    Duration duration = Duration.standardSeconds(secondsToWait);
                    String timeToWaitStr = PeriodFormat.wordBased(Locale.getDefault())
                            .print(duration.toPeriod());
                    String err = getResources().getString(R.string.please_wait_flood, timeToWaitStr);
                    userPhone.setError(err);
                } else {
                    userPhone.setError(message);
                }
            }
        }
    }
}
