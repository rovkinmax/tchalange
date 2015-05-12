package ru.korniltsev.telegram.core.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.rx.RXClient;

public class NetworkStateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RXClient rxClient = ObjectGraphService.getObjectGraph(context)
                .get(RXClient.class);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean connected = activeNetwork != null && activeNetwork.isConnected();

        rxClient.setConnected(connected);
    }
}
