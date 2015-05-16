package ru.korniltsev.telegram.core.app;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;
import dagger.ObjectGraph;
import mortar.MortarScope;
import mortar.dagger1support.ObjectGraphService;
import net.danlew.android.joda.JodaTimeAndroid;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;

/**
 * User: anatoly
 * Date: 20.04.15
 * Time: 23:45
 */
public class MyApp extends Application {
    public static final int TIMEOUT = 5000;
    private MortarScope rootScope;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        initClient();
    }

    @Override public Object getSystemService(String name) {
        if (rootScope == null) {
            rootScope = MortarScope.buildRootScope()
                    .withService(ObjectGraphService.SERVICE_NAME, ObjectGraph.create(new RootModule(this)))
                    .build("Root");
        }

        if (rootScope.hasService(name)) return rootScope.getService(name);

        return super.getSystemService(name);
    }

    private void initClient() {

    }




    public static MyApp from(Context ctx) {
        return (MyApp) ctx.getApplicationContext();
    }
    public static MyApp from(Fragment ctx) {
        return from(ctx.getActivity());
    }

    public RXClient getRxClient() {
        return null;
    }

    public RXAuthState getRxAuthState() {
        return null;
    }
}
