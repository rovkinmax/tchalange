package ru.korniltsev.telegram.core.adapters;

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
        throw new RuntimeException(th);
    }

    @Override
    public void onNext(T response) {
        Assert.fail("unhandled response: " + response );
    }
}
