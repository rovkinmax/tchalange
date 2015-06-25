package ru.korniltsev.telegram.auth.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import dagger.Provides;
import flow.Flow;
import mortar.MortarScope;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.auth.code.EnterCode;
import ru.korniltsev.telegram.auth.country.Countries;
import ru.korniltsev.telegram.auth.country.SelectCountry;
import ru.korniltsev.telegram.auth.name.EnterName;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterPhoneFragment.Module.class)
public class EnterPhoneFragment extends BasePath implements Serializable {

    private Countries.Entry c;

    public void setCountry(Countries.Entry c) {

        this.c = c;
    }

    @dagger.Module(injects = {
            EnterPhoneView.class,
            Countries.class,
    }, addsTo = RootModule.class)
    public static class Module {



    }

    @Override
    public int getRootLayout() {
        return R.layout.fragment_set_phone_number;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterPhoneView> {
        private RXClient client;
        private Observable<TdApi.TLObject> sendPhoneRequest;
        private String sentPhonenumber;

        private Subscription subscribtion = Subscriptions.empty();
        private ProgressDialog pd;
        private final Countries countries;

        @Inject
        public Presenter(RXClient client, Countries countries) {
            this.client = client;

            this.countries = countries;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            Context ctx = getView().getContext();
            EnterPhoneFragment f = get(ctx);
            Countries.Entry country;//todo save selected to pref
            if (f.c != null) {
                country = f.c;
                f.c = null;
            } else {
                country = countries//todo new
                        .getForCode(Countries.RU_CODE);

            }
            getView().countrySelected(country, true);
            if (sendPhoneRequest != null) {
                subscribe();
            }
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
            Utils.hideKeyboard(
                    getView().getPhoneCode());
        }

        public void sendCode(final String phoneNumber) {
            assertNull(sendPhoneRequest);
            sendPhoneRequest = client.logoutHelper()
                    .filter(new Func1<TdApi.AuthState, Boolean>() {
                        @Override
                        public Boolean call(TdApi.AuthState authState) {
                            return authState instanceof TdApi.AuthStateWaitSetPhoneNumber;
                        }
                    })
                    .flatMap(new Func1<TdApi.AuthState, Observable<TdApi.TLObject>>() {
                        @Override
                        public Observable<TdApi.TLObject> call(TdApi.AuthState authState) {
                            return client.sendCachedRXUI(new TdApi.AuthSetPhoneNumber(phoneNumber));
                        }
                    });

            sentPhonenumber = phoneNumber;
            subscribe();
        }

        private void subscribe() {
            pd = new ProgressDialog(getView().getContext());
            subscribtion = sendPhoneRequest.subscribe(new ObserverAdapter<TdApi.TLObject>() {
                @Override
                public void onNext(TdApi.TLObject response) {
                    Flow flow = Flow.get(getView().getContext());
                    if (response instanceof TdApi.AuthStateWaitSetCode) {
                        flow.set(new EnterCode(sentPhonenumber));
                    } else if (response instanceof TdApi.AuthStateWaitSetName){
                        flow.set(new EnterName(sentPhonenumber));
                    }
                    sendPhoneRequest = null;
                    pd.dismiss();
                }

                @Override
                public void onError(Throwable th) {
                    sendPhoneRequest = null;
                    pd.dismiss();
                    getView().showError(th.getMessage());
                }
            });
            pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    sendPhoneRequest = null;
                    subscribtion.unsubscribe();
                }
            });
            pd.show();
        }

        @Override
        public void dropView(EnterPhoneView view) {
            super.dropView(view);
            subscribtion.unsubscribe();
            if (pd != null) {
                pd.dismiss();
                pd = null;
            }
        }
    }


}
