package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.content.SharedPreferences;
import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RXAuthState {
    public static final String PREF_AUTHORIZED = "pref_authorized";
    public static final String RX_CLIENT = "rx_client";
    public static final String ME_UID = "ME_UID";
    private final SharedPreferences prefs;

    public abstract class AuthState {//todomove
//        AUTHORIZED, LOGOUT
    }

    public class StateLogout extends AuthState{

    }

    public class StateAuthorized extends AuthState{
        public final int userId;

        public StateAuthorized(int userId) {
            this.userId = userId;
        }
    }

    private final PublishSubject<AuthState> authState = PublishSubject.create();;
    final Context ctx;

    @Inject
    public RXAuthState(Context ctx) {
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences(RX_CLIENT, Context.MODE_PRIVATE);
        getState();
    }

    public AuthState getState() {
        boolean authorized = prefs.getBoolean(PREF_AUTHORIZED, false);
        if (authorized) {
            int me_uid = prefs.getInt(ME_UID, -1);
            return new StateAuthorized(me_uid);//AuthState.AUTHORIZED;
        } else {
            return new StateLogout();
        }
    }

    public void authorized(TdApi.User user) {
        prefs.edit()
                .putBoolean(PREF_AUTHORIZED, true)
                .putInt(ME_UID, user.id)
                .apply();
        authState.onNext(new StateAuthorized(user.id));
    }

    public void logout() {
        prefs.edit()
                .remove(PREF_AUTHORIZED)
                .apply();

        authState.onNext(new StateLogout());
    }

    public Observable<AuthState> listen() {
        return authState;
    }
}
