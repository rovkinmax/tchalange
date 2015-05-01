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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static junit.framework.Assert.fail;

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
            client.sendRXUI(f)
                    .subscribe( r -> {
                        if (r instanceof TdApi.AuthStateOk) {
                            auth.authorized();
                        } else {
                            fail("unimplemented");
                        }
                    });
        }
    }



}
