package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class NotificationManager {
    final RXClient client;
    final Context ctx;
    private final Ringtone ringtone;
    private final Observable<TdApi.UpdateNotificationSettings> settingsUpdate;

    @Inject
    public NotificationManager(RXClient client, Context ctx) {
        this.client = client;
        this.ctx = ctx;
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(ctx, notification);

        settingsUpdate = client.updateNotificationSettings()
                .map(new Func1<TdApi.UpdateNotificationSettings, TdApi.UpdateNotificationSettings>() {
                    @Override
                    public TdApi.UpdateNotificationSettings call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                        calculate(updateNotificationSettings.notificationSettings);
                        return updateNotificationSettings;
                    }
                })
                .observeOn(mainThread());
        settingsUpdate.subscribe(new Action1<TdApi.UpdateNotificationSettings>() {
            @Override
            public void call(TdApi.UpdateNotificationSettings upd) {
                if (upd.scope instanceof TdApi.NotificationSettingsForChat) {
                    TdApi.NotificationSettingsForChat scope = (TdApi.NotificationSettingsForChat) upd.scope;
                    settings.put(scope.chatId, upd.notificationSettings);
//                    calculate(scope.chatId, upd.notificationSettings);
                } //else todo
            }
        });
    }

    public Observable<TdApi.NotificationSettings> updatesForChat(final TdApi.Chat c) {
        return settingsUpdate.filter(new Func1<TdApi.UpdateNotificationSettings, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                if (updateNotificationSettings.scope instanceof TdApi.NotificationSettingsForChat) {
                    TdApi.NotificationSettingsForChat scope = (TdApi.NotificationSettingsForChat) updateNotificationSettings.scope;
                    return scope.chatId == c.id;
                } else {
                    return false;
                }
            }
        }).map(new Func1<TdApi.UpdateNotificationSettings, TdApi.NotificationSettings>() {
            @Override
            public TdApi.NotificationSettings call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                return updateNotificationSettings.notificationSettings;
            }
        });
    }

    Map<Long, TdApi.NotificationSettings> settings = new HashMap<>();

    public void updateNotificationScopes(List<TdApi.Chat> csList) {
        for (TdApi.Chat chat : csList) {
            long id = chat.id;
            TdApi.NotificationSettings s = chat.notificationSettings;
            calculate( s);
            settings.put(id, s);
        }
    }

    private void calculate(TdApi.NotificationSettings s) {
        long time = time();
        int secsToMute = s.muteFor;
        s.muteForElapsedRealtime = time + secsToMute * 1000;

    }

    private long time() {
        return SystemClock.elapsedRealtime();
    }

    public void notifyNewMessage(TdApi.Message msg) {
        TdApi.NotificationSettings s = this.settings.get(msg.chatId);
        if (s == null) {
            notifyNewMessageImpl();
        } else {
            if (!isMuted(s)) {
                notifyNewMessageImpl();
            }
        }
    }

    public boolean isMuted(TdApi.NotificationSettings s) {
        return time() <= s.muteForElapsedRealtime;
    }

    private void notifyNewMessageImpl() {
        try {
            ringtone.play();
        } catch (Exception e) {
            Log.e("NotificationManager", "err", e);
        }
    }

    public boolean isMuted(TdApi.Chat chat) {
        TdApi.NotificationSettings s = this.settings.get(chat.id);
        return s != null && isMuted(s);
    }

    public void mute(TdApi.Chat chat) {
        chat.notificationSettings.muteFor = 8 * 60 * 60;
        setSettings(chat);
    }



    public void unmute(TdApi.Chat chat) {
        chat.notificationSettings.muteFor = 0;
        setSettings(chat);
    }

    private void setSettings(TdApi.Chat chat) {
        TdApi.NotificationSettingsForChat scope = new TdApi.NotificationSettingsForChat(chat.id);
        calculate(chat.notificationSettings);
        settings.put(chat.id, chat.notificationSettings);
        client.sendSilently(new TdApi.SetNotificationSettings(scope, chat.notificationSettings));
    }
}
