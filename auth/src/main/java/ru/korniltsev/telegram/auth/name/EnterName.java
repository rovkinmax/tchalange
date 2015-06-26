package ru.korniltsev.telegram.auth.name;

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
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static junit.framework.Assert.assertNull;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterName.Module.class)
public class EnterName extends BasePath implements Serializable {
    private final String phoneNumber;

    public EnterName(String phoneNumber) {

        this.phoneNumber = phoneNumber;
    }

    @dagger.Module(injects = EnterNameView.class, addsTo = RootModule.class)
    public static class Module {
        final EnterName name;

        public Module(EnterName name) {
            this.name = name;
        }

        @Provides
        EnterName provideEnterName() {
            return name;
        }
    }

    @Override
    public int getRootLayout() {
        return R.layout.fragment_set_name;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterNameView> {
        private RXClient client;
        private final EnterName flow;
        private Observable<TdApi.TLObject> setNameRequest;

        private Subscription subscribtion = Subscriptions.empty();
        private Subscription currentStateSubscription = Subscriptions.empty();
        private ProgressDialog pd;


        @Inject
        public Presenter(RXClient client, EnterName flow) {
            this.client = client;
            this.flow = flow;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            if (setNameRequest != null) {
                subscribe();
            }
            currentStateSubscription = client.sendCachedRXUI(new TdApi.AuthGetState())
                    .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                        @Override
                        public void onNext(TdApi.TLObject response) {
                            if (response instanceof TdApi.AuthStateWaitSetCode) {
                                Flow.get(getView())
                                        .goBack();
                            }
                        }
                    });
        }

        public void setName(final String name, String strLastName) {
            assertNull(setNameRequest);
            setNameRequest = client.sendCachedRXUI(new TdApi.AuthSetName(name, strLastName));

            subscribe();
        }

        private void subscribe() {
            pd = new ProgressDialog(getView().getContext());
            subscribtion = setNameRequest.subscribe(new ObserverAdapter<TdApi.TLObject>() {
                @Override
                public void onNext(TdApi.TLObject response) {
                    setNameRequest = null;
                    pd.dismiss();
                    if (response instanceof TdApi.AuthStateWaitSetCode) {
                        Flow flow = Flow.get(getView().getContext());
                        flow.set(new EnterCode(Presenter.this.flow.phoneNumber));
                    }

                }

                @Override
                public void onError(Throwable th) {
                    setNameRequest = null;
                    pd.dismiss();
                    getView().showError(th.getMessage());
                }
            });
            pd.setMessage(getView().getResources().getString(R.string.please_wait));
            pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    setNameRequest = null;
                    subscribtion.unsubscribe();
                }
            });
            pd.show();
        }

        @Override
        public void dropView(EnterNameView view) {
            super.dropView(view);
            subscribtion.unsubscribe();
            currentStateSubscription.unsubscribe();

            if (pd != null) {
                pd.dismiss();
                pd = null;
            }
        }
    }
}
