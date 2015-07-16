package ru.korniltsev.telegram.profile;

import android.support.annotation.Nullable;
import dagger.Module;
import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(ProfilePath.Module.class)
public class ProfilePath extends BasePath implements Serializable{
    public final TdApi.User user;
//    @Nullable public final TdApi.Chat groupChat;

    public ProfilePath(TdApi.User user) {
        this.user = user;

    }

//    public ProfilePath(TdApi.Chat groupChat) {
//        user = null;
//        this.groupChat = groupChat;
//    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_view;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    ProfileView.class,
//                    MyTextView.class
            }
    )
    public static final class Module {
        final ProfilePath path;

        public Module(ProfilePath path) {
            this.path = path;
        }

        @Provides ProfilePath providePath() {
            return path;
        }
    }
}
