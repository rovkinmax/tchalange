package ru.korniltsev.telegram.profile;

import org.drinkless.td.libcore.telegram.TdApi;

import java.io.Serializable;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

@WithModule(ProfilePath.Module.class)
public class ProfilePath extends BasePath implements Serializable {
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
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final ProfilePath path;

        public Module(ProfilePath path) {
            this.path = path;
        }

        @Provides
        ProfilePath providePath() {
            return path;
        }
    }
}
