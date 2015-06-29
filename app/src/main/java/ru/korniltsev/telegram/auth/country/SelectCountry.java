package ru.korniltsev.telegram.auth.country;

import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.auth.phone.EnterPhoneFragment;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.flow.utils.Utils;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

/**
 * Created by korniltsev on 22/04/15.
 */
@WithModule(SelectCountry.Module.class)
public class SelectCountry extends BasePath implements Serializable {
    @Override
    public int getRootLayout() {
        return R.layout.auth_select_country_view;
    }

    @dagger.Module(
            injects = {
                    SelectCountryView.class
            }, addsTo = RootModule.class)
    public static class Module {

    }

    @Singleton
    static class Presenter extends ViewPresenter<SelectCountryView> {
        @Inject
        public Presenter() {
        }

        public void countrySelected(Countries.Entry c) {
            EnterPhoneFragment prev = Utils.getPreviousPath(getView());
            prev.setCountry(c);
            Flow.get(getView())
                    .goBack();
        }
    }

}
