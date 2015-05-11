package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import org.drinkless.td.libcore.telegram.TdApi;

import javax.inject.Inject;
import javax.inject.Singleton;

import static junit.framework.Assert.assertTrue;

@Singleton
public class RxGlide {

    public static final String TELEGRAM_FILE = "telegram.file.";
    private final Picasso picasso;

    private Context ctx;




    @Inject
    public RxGlide(Context ctx, RxDownloadManager downlaoder) {
        this.ctx = ctx;

        picasso = new Picasso.Builder(ctx)
                .addRequestHandler(new StubRequestHandler(ctx))
                .addRequestHandler(new TDFileRequestHandler(downlaoder))
                .build();
    }

    private static final RxGlide.StubAware<TdApi.GroupChat> STUB_AWARE_GROUP_CHAT = new StubAware<TdApi.GroupChat>() {
        @Override
        public String needStub(TdApi.GroupChat o) {
            TdApi.GroupChat chat = o;
            String title = chat.title;
            if (title.length() > 0) {
                return String.valueOf(
                        Character.toUpperCase(title.charAt(0)));
            }
            return "";
        }
    };

    private static final RxGlide.StubAware<TdApi.User> STUB_AWARE_USER = new StubAware<TdApi.User>() {
        @Override
        public String needStub(TdApi.User o) {
            TdApi.User user = o;
            StringBuilder sb = new StringBuilder();
            if (user.firstName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.firstName.charAt(0)));
            }
            if (user.lastName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.lastName.charAt(0)));
            }
            return sb.toString();
        }
    };

    public RequestCreator loadAvatarForUser(TdApi.User u, int size) {
        TdApi.File file = u.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                return loadStub(u, size);
            }
        }
        return loadPhoto(file, false)
                .resize(size, size);
    }

    /**
     *
     * @param u
     * @param size in px
     * @return
     */
    private RequestCreator loadStub(TdApi.User u, int size) {
        String chars = STUB_AWARE_USER.needStub(u);
        Uri uri = StubRequestHandler.create(chars, u.id, size);
        return picasso.load(uri);

    }

    private RequestCreator loadStub(TdApi.GroupChatInfo info, int size) {
        String chars = STUB_AWARE_GROUP_CHAT.needStub(info.groupChat);
        Uri uri = StubRequestHandler.create(chars, info.groupChat.id, size);
        return picasso.load(uri);
    }

    public RequestCreator loadAvatarForChat(TdApi.Chat chat, int size) {
        if (chat.type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chat.type).user;
            return loadAvatarForUser(user, size);
        } else {
            return loadAvatarForGroup(chat, size);
        }
    }

    private RequestCreator loadAvatarForGroup(TdApi.Chat chat, int size) {
        TdApi.GroupChatInfo info = (TdApi.GroupChatInfo) chat.type;
        TdApi.File file = info.groupChat.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                return loadStub(info, size);
            }
        }
        return loadPhoto(file, false)
                .resize(size, size);
    }



    public RequestCreator loadPhoto(TdApi.File f, boolean webp) {
        if (f instanceof TdApi.FileEmpty){
            TdApi.FileEmpty e = (TdApi.FileEmpty) f;
            assertTrue(e.id != 0);
        }
        return picasso.load(TDFileRequestHandler.load(f, webp))
                .stableKey(stableKeyForTdApiFile(f, webp));

    }

    private String stableKeyForTdApiFile(TdApi.File f, boolean webp) {
        int id;
        if (f instanceof TdApi.FileLocal){
            id = ((TdApi.FileLocal) f).id;
        } else {
            id = ((TdApi.FileEmpty) f).id;
        }
        return String.format("id=%d&webp=%b", id, webp);
    }

    public interface StubAware<T> {
        String needStub(T o);
    }

    public static String id(TdApi.FileLocal f) {
        return TELEGRAM_FILE + f.id;
    }

    public static String id(TdApi.FileEmpty f) {
        return TELEGRAM_FILE + f.id;
    }

    //user only to load not td related stuff
    public Picasso getPicasso() {
        return picasso;
    }
}
