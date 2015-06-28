package ru.korniltsev.telegram.auth.name;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class EnterNameView extends LinearLayout {
//    private EditText btnSelectCountry;
//    private EditText phoneCode;
    private EditText name;
    @Inject EnterName.Presenter presenter;
    private EditText lastName;
    //    @Inject ActivityOwner owner;

    public EnterNameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initToolbar(this)
                .setTitle(R.string.enter_name)
                .addMenuItem(
                        R.menu.auth_send_code,
                        R.id.menu_send_code,
                        new Runnable() {
                            @Override
                            public void run() {
                                setName();
                            }
                        }
                );
        name = (EditText) findViewById(R.id.first_name);
        lastName = (EditText) findViewById(R.id.last_name);

        name.requestFocus();
        lastName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setName();
                    return true;
                }
                return false;
            }
        });
    }

    private void setName() {
        String strName = textFrom(name);
        String strLastName = textFrom(lastName);
        presenter.setName(strName, strLastName);
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



    public void showError(String message) {
        name.setError(message);
    }
}
