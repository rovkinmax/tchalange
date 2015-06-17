package ru.korniltsev.telegram.auth.code;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Telephony;
import dagger.Provides;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.Utils.hideKeyboard;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterCode.Module.class)
public class EnterCode extends BasePath implements Serializable{

    private static Subscription smsSubscription;
    public final String phoneNumber;

    public EnterCode(String phoneNumber) {

        this.phoneNumber = phoneNumber;
    }

    @dagger.Module(injects = EnterCodeView.class, addsTo = RootModule.class)
    public static class Module {
        final EnterCode code;


        public Module(EnterCode code) {
            this.code = code;
        }

        @Provides EnterCode provideEnterCode() {
            return code;
        }
    }
    @Override
    public int getRootLayout() {
        return R.layout.fragment_set_code;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterCodeView>{
        private final EnterCode path;
        private final RXClient client;
        private final RXAuthState auth;
        private Observable<TdApi.User> request;
        private Subscription subscription= Subscriptions.empty();
        private ProgressDialog pd;
        //        private ProgressDialog pd;

        @Inject
        public Presenter(EnterCode path, RXClient client, RXAuthState auth) {
            this.path = path;
            this.client = client;
            this.auth = auth;
        }

        public EnterCode getPath() {
            return path;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            if (request != null){
                subscribe();
            }
            smsSubscription = ContentObservable.fromBroadcast(
                    getView().getContext(),
                    new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
                    .subscribe(new Action1<Intent>() {
                        @Override
                        public void call(Intent intent) {
                            handleSms(intent);
                        }
                    });
        }

        private void handleSms(Intent intent) {
            List<String> messages = SMSReceiver.getMessages(intent);
            Pattern p = Pattern.compile("Telegram code (\\d+)");
            for (String msg : messages) {
                Matcher m = p.matcher(msg);
                if (m.matches()){
                    String code = m.group(1);
                    if (getView().getSmsCode().getText().length() == 0){
                        checkCode(code);
                    }
                    System.out.println(code);
                    break;
                }
            }
        }

        @Override
        public void dropView(EnterCodeView view) {
            super.dropView(view);
            subscription.unsubscribe();
            smsSubscription.unsubscribe();
            if (pd != null){
                pd.dismiss();
            }
        }

        public void checkCode(String code) {
            assertNull(request);
            TdApi.AuthSetCode f = new TdApi.AuthSetCode(code);
            request = authorizeAndGetMe(f);

            subscribe();
        }

        private void subscribe() {
            hideKeyboard(getView()
                    .getSmsCode());
            pd = new ProgressDialog(getView().getContext());
            subscription = request.subscribe(new ObserverAdapter<TdApi.User>() {
                @Override
                public void onError(Throwable th) {
                    getView().showError(th);
                    pd.dismiss();
                    request = null;
                }

                @Override
                public void onNext(TdApi.User response) {
                    auth.authorized(response);
                    pd.dismiss();
                    request = null;
                }
            });

            pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    subscription.unsubscribe();
                    request = null;
                }
            });
            pd.show();
        }

        private Observable<TdApi.User> authorizeAndGetMe(TdApi.AuthSetCode f) {
            return client.sendRx(f)
                    .map(new Func1<TdApi.TLObject, TdApi.AuthStateOk>() {
                        @Override
                        public TdApi.AuthStateOk call(TdApi.TLObject tlObject) {
                            return (TdApi.AuthStateOk) tlObject;
                        }
                    })
                    .flatMap(new Func1<TdApi.AuthStateOk, Observable<TdApi.TLObject>>() {
                        @Override
                        public Observable<TdApi.TLObject> call(TdApi.AuthStateOk authStateOk) {
                            return client.sendRx(new TdApi.GetMe());
                        }
                    }).map(new Func1<TdApi.TLObject, TdApi.User>() {
                        @Override
                        public TdApi.User call(TdApi.TLObject tlObject) {
                            return (TdApi.User) tlObject;
                        }
                    })
                    .cache()
                    .observeOn(mainThread());
        }
    }



}
