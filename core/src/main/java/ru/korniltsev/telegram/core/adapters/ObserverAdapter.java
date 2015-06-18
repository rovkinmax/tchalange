package ru.korniltsev.telegram.core.adapters;

import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import junit.framework.Assert;
import rx.Observer;

/**
 * Created by korniltsev on 21/04/15.
 */
public class ObserverAdapter<T> implements Observer<T> {
    @Override
    public final void onCompleted() {

    }

    @Override
    public void onError(Throwable th) {
        CrashlyticsCore.getInstance().logException(th);
        Log.e("ObserverAdapter", "err", th);
    }

    @Override
    public void onNext(T response) {

    }
}
