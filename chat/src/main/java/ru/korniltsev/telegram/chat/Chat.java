package ru.korniltsev.telegram.chat;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.AudioMessageView;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.chat.adapter.view.EmojiTextView;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(Chat.Module.class)
public class Chat extends BasePath implements Serializable {

    public static final int LIMIT = 15;

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


    @dagger.Module(
            injects = {
                    ChatView.class,
                    PhotoMessageView.class,
                    EmojiTextView.class ,
                    AudioMessageView.class ,
                    GeoPointView.class ,
                    VideoView.class ,
                    DocumentView.class ,
            },
            addsTo = RootModule.class)
    public static class Module {

    }
}
