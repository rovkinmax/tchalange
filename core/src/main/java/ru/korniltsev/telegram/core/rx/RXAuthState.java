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
    private final SharedPreferences prefs;
    private final RXClient client;

    public enum AuthState {//todomove
        AUTHORIZED, LOGOUT
    }

    private final PublishSubject<AuthState> authState = PublishSubject.create();;
    final Context ctx;

    @Inject
    public RXAuthState( Context ctx, RXClient client) {
        this.ctx = ctx;
        this.client = client;
        prefs = ctx.getSharedPreferences(RX_CLIENT, Context.MODE_PRIVATE);
        getState();
    }

    public AuthState getState() {
        return prefs.getBoolean(PREF_AUTHORIZED, false) ? AuthState.AUTHORIZED : AuthState.LOGOUT;
    }

    public void authorized() {
        prefs.edit()
                .putBoolean(PREF_AUTHORIZED, true)
                .commit();
        authState.onNext(AuthState.AUTHORIZED);
    }

    public void logout() {
        client.sendSilently(new TdApi.AuthReset());
        prefs.edit()
                .remove(PREF_AUTHORIZED)
                .commit();
        //todo clear picasso cache
        // but not here!
        authState.onNext(AuthState.LOGOUT);
    }

    public Observable<AuthState> listen() {
        return authState;
    }
}
