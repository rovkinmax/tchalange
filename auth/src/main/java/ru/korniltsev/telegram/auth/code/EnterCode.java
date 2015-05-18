package ru.korniltsev.telegram.auth.code;

import android.os.Bundle;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static junit.framework.Assert.fail;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterCode.Module.class)
public class EnterCode extends BasePath implements Serializable{

    @dagger.Module(injects = EnterCodeView.class, addsTo = RootModule.class)
    public static class Module {

    }
    @Override
    public int getRootLayout() {
        return R.layout.fragment_set_code;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterCodeView>{
        private final RXClient client;
        private final RXAuthState auth;

        @Inject
        public Presenter(RXClient client, RXAuthState auth) {
            this.client = client;
            this.auth = auth;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
        }

        public void checkCode(String code) {
            TdApi.AuthSetCode f = new TdApi.AuthSetCode(code);
            authorizeAndGetMe(f)
                    .observeOn(mainThread())
                    .subscribe(new Action1<TdApi.User>() {
                        @Override
                        public void call(TdApi.User user) {
                            auth.authorized(user);
                        }
                    });
//                    .subscribe(new Action1<TdApi.TLObject>() {
//                        @Override
//                        public void call(TdApi.TLObject r) {
//                            if (r instanceof TdApi.AuthStateOk) {
//
//                            } else {
//                                fail("unimplemented");
//                            }
//                        }
//                    });
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
            });
        }
    }



}
