package com.example.activity;

import android.app.Activity;
import android.os.Bundle;
import flow.Flow;
import flow.FlowDelegate;
import flow.History;
import mortar.MortarScope;
import mortar.bundler.BundleServiceRunner;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.R;
import ru.korniltsev.telegram.auth.phone.EnterPhoneFragment;
import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.core.flow.SerializableParceler;
import ru.korniltsev.telegram.core.mortar.core.MortarScreenSwitcherFrame;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import rx.Subscription;
import rx.functions.Action1;

import static mortar.bundler.BundleServiceRunner.getBundleServiceRunner;

public class MainActivity extends Activity {

    private MortarScreenSwitcherFrame container;
    private FlowDelegate flow;
    private MortarScope activityScope;

    RXAuthState authState;
    private Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.root_layout);
        container = ((MortarScreenSwitcherFrame) findViewById(R.id.container));

        MortarScope parentScope = MortarScope.getScope(getApplication());

        String scopeName = getLocalClassName() + "-task-" + getTaskId();

        activityScope = parentScope.findChild(scopeName);
        if (activityScope == null) {
            activityScope = parentScope.buildChild()
                    .withService(BundleServiceRunner.SERVICE_NAME, new BundleServiceRunner())
                    .build(scopeName);
        }

//        GsonParceler parceler = new GsonParceler(new Gson());
        @SuppressWarnings("deprecation") FlowDelegate.NonConfigurationInstance nonConfig =
                (FlowDelegate.NonConfigurationInstance) getLastNonConfigurationInstance();


        authState = ObjectGraphService.getObjectGraph(this).get(RXAuthState.class);
        History history = History.single(getScreenForAuthState(authState.getState()));
        flow = FlowDelegate.onCreate(nonConfig, getIntent(), savedInstanceState, new SerializableParceler(),
                history, container);

        getBundleServiceRunner(activityScope).onCreate(savedInstanceState);


//        getWindow().getDecorView().setBackgroundDrawable(null);
    }

    private Object getScreenForAuthState(RXAuthState.AuthState state) {
        return state == RXAuthState.AuthState.AUTHORIZED ? new ChatList() : new EnterPhoneFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        flow.onResume();
        subscription = authState.listen()
                .subscribe(new Action1<RXAuthState.AuthState>() {
                    @Override
                    public void call(RXAuthState.AuthState s) {
                        History history = History.single(
                                MainActivity.this.getScreenForAuthState(s));
                        Flow.get(MainActivity.this)
                                .setHistory(history, Flow.Direction.REPLACE);
                    }
                });


    }

    @Override
    protected void onPause() {
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
            return activityScope.getService(name);
        }
        return super.getSystemService(name);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        flow.onSaveInstanceState(outState);
        getBundleServiceRunner(this).
                onSaveInstanceState(outState);
    }

    /**
     * Inform the view about back events.
     */
    @Override
    public void onBackPressed() {
        if (!container.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        // activityScope may be null in case isWrongInstance() returned true in onCreate()
        if (isFinishing() && activityScope != null) {
            activityScope.destroy();
            activityScope = null;
        }

        super.onDestroy();
    }


}
