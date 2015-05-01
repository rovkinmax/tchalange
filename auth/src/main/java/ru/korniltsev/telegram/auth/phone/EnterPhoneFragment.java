package ru.korniltsev.telegram.auth.phone;

import android.os.Bundle;
import flow.Flow;
import mortar.MortarScope;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.auth.code.EnterCode;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.auth.country.SelectCountry;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterPhoneFragment.Module.class)
public class EnterPhoneFragment extends BasePath implements Serializable{

    private Countries.Entry c;

    public void setCountry(Countries.Entry c) {

        this.c = c;
    }

    @dagger.Module(injects = EnterPhoneView.class, addsTo = RootModule.class)
    public static class Module {

    }
    @Override
    public int getRootLayout() {
        return R.layout.fragment_set_phone_number;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterPhoneView>{
        private RXClient client;

        @Inject
        public Presenter(RXClient client) {
            this.client = client;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            EnterPhoneFragment f = get(getView().getContext());
            Countries.Entry country;//todo save selected to pref
            if (f.c != null){
                country = f.c;
            } else {
                country = new Countries(getView().getContext())//todo new
                        .getForCode(Countries.RU_CODE);
            }
            getView().countrySelected(country);

        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            super.onEnterScope(scope);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
        }

        public void selectCountry() {
            Flow.get(getView())
                    .set(new SelectCountry());
        }

        public void sendCode(String phoneNumber) {
                    client.sendRXUI(new TdApi.AuthSetPhoneNumber(phoneNumber))
                    .subscribe(new Action1<TdApi.TLObject>() {
                        @Override
                        public void call(TdApi.TLObject response) {
                            if (response instanceof TdApi.AuthStateWaitSetCode) {
                                Flow.get(Presenter.this.getView().getContext())
                                        .set(new EnterCode());
                            }
                        }
                    });
        }


    }



    //    private EditText btnSelectCountry;
//    private EditText phoneCode;
//    private EditText userPhone;
//    private Toolbar toolbar;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_set_phone_number, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        initToolbar(view)
//                .setTitle(R.string.phone_number)
//                .addMenuItem(R.menu.send_code, R.id.menu_send_code, new Runnable() {
//                    @Override
//                    public void run() {
//                        sendCode();
//                    }
//                });
//
//        btnSelectCountry = (EditText) view.findViewById(R.id.btn_select_country);
//        phoneCode = (EditText) view.findViewById(R.id.country_phone_code);//todo editable
//        userPhone = (EditText) view.findViewById(R.id.user_phone);
//        btnSelectCountry.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                openCountrySelection();
//            }
//        });
//        Countries.Entry russia = new Countries(getActivity())//todo new
//                .getForCode(Countries.RU_CODE);//todo save selected to pref
//        countrySelected(russia);
//    }
//
//    private void sendCode() {
//        getRxClient().sendRXUI(new TdApi.AuthSetPhoneNumber(getPhoneNumber()))
//        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
//            @Override
//            public void onNext(TdApi.TLObject response) {
//                if (response instanceof TdApi.AuthStateWaitSetCode) {
//                    FlowLike.from(getActivity())
//                            .push(new SetCodeFragment(), "set code");
//                }
//            }
//
//        });
//    }
//
//    private String getPhoneNumber() {
//        return textFrom(phoneCode) + textFrom(userPhone);
//    }
//
//    private void countrySelected(Countries.Entry c) {
//        btnSelectCountry.setText(c.name);
//        phoneCode.setText(c.phoneCode);
//    }
//
//    private void openCountrySelection() {
//        FlowLike.from(getActivity())
//                .push(new CountrySelectFragment(), "select country");
//    }
//
//    @Override
//    public void onResult(Object result) {
//        countrySelected((Countries.Entry) result);
//    }
}
