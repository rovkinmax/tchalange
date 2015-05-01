package ru.korniltsev.telegram.core.flow;

import android.support.v4.app.Fragment;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.rx.RXClient;

/**
 * Created by korniltsev on 22/04/15.
 */
public class BaseFragment extends Fragment {
    public BaseFragment() {
        setRetainInstance(true);
    }

    public void onResult(Object result) {

    }

    protected RXClient getRxClient() {
        return MyApp.from(this)
                .getRxClient();
    }
}
