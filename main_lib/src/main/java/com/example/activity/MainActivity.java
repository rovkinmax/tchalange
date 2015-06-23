package com.example.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import dagger.ObjectGraph;
import flow.Flow;
import flow.FlowDelegate;
import flow.History;
import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.R;
import ru.korniltsev.telegram.auth.phone.EnterPhoneFragment;
import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.flow.SerializableParceler;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.mortar.core.MortarScreenSwitcherFrame;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import static mortar.bundler.BundleServiceRunner.getBundleServiceRunner;
import static ru.korniltsev.telegram.core.Utils.event;

public class MainActivity extends ActionBarActivity implements ActivityOwner.AnActivity {

    private MortarScreenSwitcherFrame container;
    private FlowDelegate flow;
    private MortarScope activityScope;

    RXAuthState authState;
    private Subscription subscription;
    private ActivityOwner activityOwner;
    private static boolean firstRun = true;
    private BundleServiceRunner bundleServiceRunner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (firstRun) {
            savedInstanceState = null;
            firstRun = false;//todo remove when libtd has stable ids across process instances
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.root_layout);
        container = ((MortarScreenSwitcherFrame) findViewById(R.id.container));

        MortarScope parentScope = MortarScope.getScope(getApplication());

        String scopeName = getLocalClassName() + "-task-" + getTaskId();

        activityScope = parentScope.findChild(scopeName);
        event("activityScope == " + activityScope);
        if (activityScope == null) {
            activityScope = parentScope.buildChild()
                    .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
                    .build(scopeName);
            event("activityScope == " + activityScope);
        }

//        GsonParceler parceler = new GsonParceler(new Gson());
        @SuppressWarnings("deprecation") FlowDelegate.NonConfigurationInstance nonConfig =
                (FlowDelegate.NonConfigurationInstance) getLastNonConfigurationInstance();

        ObjectGraph objectGraph = ObjectGraphService.getObjectGraph(this);
        authState = objectGraph.get(RXAuthState.class);
        activityOwner = objectGraph.get(ActivityOwner.class);
        activityOwner.takeView(this);
        History history = History.single(getScreenForAuthState(authState.getState()));
        flow = FlowDelegate.onCreate(nonConfig, getIntent(), savedInstanceState, new SerializableParceler(),
                history, container);

        bundleServiceRunner = getBundleServiceRunner(activityScope);
        bundleServiceRunner.onCreate(savedInstanceState);

        getWindow().getDecorView().setBackgroundDrawable(null);
    }

    private Object getScreenForAuthState(RXAuthState.AuthState state) {
        if (state instanceof RXAuthState.StateAuthorized) {
            RXAuthState.StateAuthorized a = (RXAuthState.StateAuthorized) state;
            return new ChatList(a.userId);
        } else {
            return new EnterPhoneFragment();
        }
//        return state == RXAuthState.AuthState.AUTHORIZED ? new ChatList() : new EnterPhoneFragment();
    }

    @Override
    protected void onResume() {
        event("onResume");
        super.onResume();
        flow.onResume();
        subscription = authState.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState s) {
                        History history = History.single(
                                MainActivity.this.getScreenForAuthState(s));
                        Flow.get(MainActivity.this)
                                .setHistory(history, Flow.Direction.REPLACE);
                    }
                });


    }

    @Override
    protected void onPause() {
        event("onPause");
        super.onPause();
        flow.onPause();
        subscription.unsubscribe();
    }

    @Override
    public Object getSystemService(String name) {
        if (flow != null) {
            Object service = flow.getSystemService(name);
            if (service != null) {
                return service;
            }
        }
        if (activityScope != null && activityScope.hasService(name)) {
            Object service = activityScope.getService(name);
            if (service == null) {
                event("return null service");
                event("activityScope.isDestroyed()" + activityScope.isDestroyed());
            }
            return service;
        }
        return super.getSystemService(name);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        event("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        flow.onSaveInstanceState(outState);
        BundleServiceRunner service = getBundleServiceRunner(this);
        service.onSaveInstanceState(outState);
    }

    /**
     * Inform the view about back events.
     */
    @Override
    public void onBackPressed() {
        event("onBackPressed");
        if (!container.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        event("activity.onDestroy()");
        activityOwner.dropView(this);
        // activityScope may be null in case isWrongInstance() returned true in onCreate()
        if (isFinishing() && activityScope != null) {
            event("destroy activity scope");
            activityScope.destroy();
            activityScope = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        activityResult.onNext(new ActivityResult(requestCode, resultCode, data));
    }


    private PublishSubject<ActivityResult> activityResult = PublishSubject.create();

    @Override
    public Activity expose() {
        return this;
    }

    @Override
    public Observable<ActivityResult> activityResult() {
        return activityResult;
    }
}
