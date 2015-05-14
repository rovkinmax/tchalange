package ru.korniltsev.telegram.core.mortar;

import android.app.Activity;
import mortar.Presenter;
import mortar.bundler.BundleService;

public class ActivityOwner extends Presenter<Activity> {

    @Override
    protected BundleService extractBundleService(Activity view) {
        return BundleService.getBundleService(view);
    }

    public Activity expose() {
        return getView();
    }
}
