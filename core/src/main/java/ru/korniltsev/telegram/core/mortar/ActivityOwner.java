package ru.korniltsev.telegram.core.mortar;

import android.app.Activity;
import mortar.Presenter;
import mortar.bundler.BundleService;
import rx.Observable;

public class ActivityOwner extends Presenter<ActivityOwner.AnActivity> {

    public interface AnActivity {
        Activity expose();
        Observable<ActivityResult> activityResult();

        void setStatusBarColor(int color);
    }

    @Override
    protected BundleService extractBundleService(AnActivity view) {
        return BundleService.getBundleService(view.expose());
    }

    public Activity expose() {
        return getView().expose();
    }

    public Observable<ActivityResult> activityResult() {
        return getView().activityResult();
    }

    public void setStatusBarColor(int color) {
        getView().setStatusBarColor(color);
    }
}
