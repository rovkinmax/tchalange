package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertNull;

@WithModule(Chat.Module.class)
public class Chat extends BasePath implements Serializable {

    public static final int LIMIT = 25;

    public final TdApi.Chat chat;
    public final TdApi.User me;

    public Chat(TdApi.Chat chat, TdApi.User me) {
        this.chat = chat;
        this.me = me;
    }

    @Override
    public int getRootLayout() {
        return R.layout.fragment_chat;
    }


    @dagger.Module(injects = ChatView.class, addsTo = RootModule.class)
    public static class Module {

    }
}
