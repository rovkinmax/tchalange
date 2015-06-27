package ru.korniltsev.telegram.photoview;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import dagger.Provides;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.GalleryService;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static junit.framework.Assert.assertTrue;

@WithModule(PhotoView.Module.class)
public class PhotoView extends BasePath implements Serializable {

    public static final int NO_MESSAGE = 0;
    public final TdApi.Photo photo;
    public final int messageId;
    public final long chatId;

    public PhotoView(TdApi.Photo photo) {
        this.photo = photo;
        messageId = NO_MESSAGE;
        chatId = NO_MESSAGE;
    }

    public PhotoView(TdApi.Photo photo, int msgId, long chatId) {
        this.photo = photo;
        this.messageId = msgId;
        this.chatId = chatId;
    }

    @Override
    public int getRootLayout() {
        return R.layout.view_photo_view;
    }

    @dagger.Module(
            injects = {
                    PhotoViewView.class,
            },
            addsTo = RootModule.class)
    public static class Module {
        final PhotoView path;

        public Module(PhotoView path) {
            this.path = path;
        }

        @Provides
        public PhotoView providePath() {
            return path;
        }
    }
}
