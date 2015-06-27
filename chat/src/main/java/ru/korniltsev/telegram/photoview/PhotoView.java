package ru.korniltsev.telegram.photoview;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

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
        return R.layout.photo_view_view;
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
