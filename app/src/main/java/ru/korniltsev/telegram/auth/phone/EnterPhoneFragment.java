package ru.korniltsev.telegram.auth.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import dagger.Provides;
import flow.Flow;
import mortar.MortarScope;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
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
import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterPhoneFragment.Module.class)
public class EnterPhoneFragment extends BasePath implements Serializable {

    private Countries.Entry c;
    private int loadCounter = 0;


    public void setCountry(Countries.Entry c) {

        this.c = c;
    }

    @dagger.Module(injects = {
            EnterPhoneView.class,
            Countries.class,
    }, addsTo = RootModule.class)
    public static class Module {
        final EnterPhoneFragment path;

        public Module(EnterPhoneFragment path) {
            this.path = path;
        }

        @Provides public EnterPhoneFragment providePath(){
            return path;
        }
    }

    @Override
    public int getRootLayout() {
        return R.layout.auth_set_phone_number_view;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterPhoneView> {
        private RXClient client;
        private Observable<TdApi.TLObject> sendPhoneRequest;
        private String sentPhonenumber;

        private Subscription subscribtion = Subscriptions.empty();
        private ProgressDialog pd;
        private final Countries countries;
        final EnterPhoneFragment path;
        @Inject
        public Presenter(RXClient client, Countries countries, EnterPhoneFragment path) {
            this.client = client;

            this.countries = countries;
            this.path = path;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            path.loadCounter++;
            Context ctx = getView().getContext();
            selectCountry(ctx);
            if (sendPhoneRequest != null) {
                subscribe();
            }
        }

        private void selectCountry(Context ctx) {
            EnterPhoneFragment f = get(ctx);
            Countries.Entry country = null;//todo save selected to pref

            Locale locale = Locale.getDefault();
            if (path.loadCounter == 1){
                if (locale != null && locale.getLanguage().equals("ru")){
                    country = countries.getForCode(Countries.RU_CODE);
                }
            } else {
                if (f.c != null) {
                    country = f.c;
                    f.c = null;
                }
            }
            getView().countrySelected(country, true);

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
            sendPhoneRequest = setPhoneObservable(phoneNumber)
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends TdApi.TLObject>>() {
                @Override
                public Observable<? extends TdApi.TLObject> call(Throwable throwable) {
                    String message = throwable.getMessage();
                    if (message.contains("Cannot change phone number after authorization or entered code")) {
                        return client.sendRx(new TdApi.AuthReset())
                                .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.TLObject>>() {
                                    @Override
                                    public Observable<TdApi.TLObject> call(TdApi.TLObject tlObject) {
                                        return setPhoneObservable(phoneNumber);
                                    }
                                });
                    } else {
                        return Observable.error(throwable);
                    }
                }
            });

            sentPhonenumber = phoneNumber;
            subscribe();
        }

        @NonNull
        private Observable<TdApi.TLObject> setPhoneObservable(final String phoneNumber) {
            return client.logoutHelper()
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
            pd.setMessage(getView().getResources().getString(R.string.please_wait));
            pd.setCanceledOnTouchOutside(false);
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
            path.c = view.getSelectedCountry();
            subscribtion.unsubscribe();
            if (pd != null) {
                pd.dismiss();
                pd = null;
            }
        }
    }


}
