package ru.korniltsev.telegram.core.app;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import dagger.ObjectGraph;
import mortar.MortarScope;
import mortar.dagger1support.ObjectGraphService;
import net.danlew.android.joda.JodaTimeAndroid;
import ru.korniltsev.telegram.core.emoji.Stickers;
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

        final Thread.UncaughtExceptionHandler w = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                if (ex instanceof InflateException) {
                    w.uncaughtException(thread, ex.getCause().getCause());
                } else {
                    w.uncaughtException(thread, ex);
                }
            }
        });
    }

    @Override public Object getSystemService(String name) {
        if (rootScope == null) {
            ObjectGraph graph = ObjectGraph.create(new RootModule(this));
            rootScope = MortarScope.buildRootScope()
                    .withService(ObjectGraphService.SERVICE_NAME, graph)
                    .build("Root");
            graph.get(Stickers.class);//todo better solution
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
