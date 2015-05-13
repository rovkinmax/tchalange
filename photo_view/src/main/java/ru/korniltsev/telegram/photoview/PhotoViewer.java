package ru.korniltsev.telegram.photoview;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import dagger.Provides;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.GalleryService;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.Serializable;

import static junit.framework.Assert.assertTrue;

@WithModule(PhotoViewer.Module.class)
public class PhotoViewer extends BasePath implements Serializable {

    public static final int NO_MESSAGE = 0;
    private final TdApi.Photo photo;
    private final int messageId;
    private final long chatId;

    public PhotoViewer(TdApi.Photo photo) {
        this.photo = photo;
        messageId = NO_MESSAGE;
        chatId = NO_MESSAGE;
    }

    public PhotoViewer(TdApi.Photo photo, int msgId, long chatId) {
        this.photo = photo;
        this.messageId = msgId;
        this.chatId = chatId;
    }

    @Override
    public int getRootLayout() {
        return R.layout.view_photo_viewer;
    }

    @dagger.Module(
            injects = {
                    PhotoViewerView.class,
            },
            addsTo = RootModule.class)
    public static class Module {
        final PhotoViewer path;

        public Module(PhotoViewer path) {
            this.path = path;
        }

        @Provides
        public PhotoViewer providePath() {
            return path;
        }
    }

    @Singleton
    public static class Presenter extends ViewPresenter<PhotoViewerView> {

        final PhotoViewer path;
        final RXClient client;
        final ChatDB chats;
        final GalleryService galleryService;

        private Observable<TdApi.TLObject> deleteRequest;
        private CompositeSubscription subs;

        @Inject
        public Presenter(PhotoViewer path, RXClient client, ChatDB chats, GalleryService galleryService) {
            this.path = path;
            this.client = client;
            this.chats = chats;
            this.galleryService = galleryService;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            Context ctx = getView().getContext();
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            TdApi.File f = PhotoUtils.findSmallestBiggerThan(path.photo, width, height);
            getView()
                    .show(f);
            if (NO_MESSAGE == path.messageId) {
                getView()
                        .hideDeleteMessageMenuItem();
            }
            subscribe();
        }

        private void subscribe() {
            subs = new CompositeSubscription();
            subscribeForDeletition();
        }

        public void deleteMessage() {
            assertTrue(path.messageId != NO_MESSAGE);
            RxChat chat = chats.getRxChat(path.chatId);
            deleteRequest = chat.deleteMessage(path.messageId);
            subscribeForDeletition();
        }

        private void subscribeForDeletition() {
            if (deleteRequest != null) {
                subs.add(
                        deleteRequest.subscribe(new Action1<TdApi.TLObject>() {
                            @Override
                            public void call(TdApi.TLObject tlObject) {
                                Flow.get(getView())
                                        .goBack();
                            }
                        }));
            }
        }

        @Override
        public void dropView(PhotoViewerView view) {
            super.dropView(view);
            subs.unsubscribe();
        }

        public void saveToGallery() {
            galleryService.saveToGallery(path.photo)
                    .subscribe();
        }
    }
}
