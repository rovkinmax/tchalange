package ru.korniltsev.telegram.core.picasso;

import android.content.Context;
import android.widget.ImageView;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.core.views.RoundTransformation;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;

@Singleton
public class RxGlide {
    public static final RoundTransformation ROUND = new RoundTransformation();

    public static final String TELEGRAM_FILE = "telegram.file.";
    private final Picasso picasso;
    private final LruCache cache;

    private Context ctx;

    @Inject
    public RxGlide(Context ctx, RxDownloadManager downlaoder, RXAuthState auth) {
        this.ctx = ctx;

        cache = new LruCache(Utils.calculateMemoryCacheSize(ctx));
        picasso = new Picasso.Builder(ctx)
                .memoryCache(cache)
                .addRequestHandler(new TDFileRequestHandler(downlaoder))
                .build();

        auth.listen().subscribe(new Action1<RXAuthState.AuthState>() {
            @Override
            public void call(RXAuthState.AuthState authState) {
                if (authState instanceof RXAuthState.StateLogout){
                    cache.clear();
                }
            }
        });
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

    public void loadAvatarForUser(TdApi.User u, int size, AvatarView avatarView) {
        TdApi.File file = u.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                loadStub(u, size,avatarView);
                return;
            }
        }
        loadPhoto(file, false)
                .resize(size, size)
                .transform(ROUND)
                .placeholder(getStubDrawable(u, size))
                .into(avatarView);
    }

    /**
     *
     * @param u
     * @param size in px
     * @return
     */
    private void loadStub(TdApi.User u, int size, ImageView target) {
        StubDrawable stub = getStubDrawable(u, size);
        target.setImageDrawable(stub);
        picasso.cancelRequest(target);
    }

    private StubDrawable getStubDrawable(TdApi.User u, int size) {
        String chars = STUB_AWARE_USER.needStub(u);

        return getStubDrawable(chars, u.id, size);
    }

    private void stubDrawable(String chars, int id, int size, ImageView target) {
        StubDrawable stub = getStubDrawable(chars, id, size);
        target.setImageDrawable(stub);
        picasso.cancelRequest(target);
    }

    private StubDrawable getStubDrawable(String chars, int id, int size) {
        StubKey key = new StubKey(id, chars, size);
        StubDrawable stub = stubs.get(key);
        if (stub == null) {
            stub = new StubDrawable(key);
            stubs.put(key, stub);
        }
        return stub;
    }

    private void loadStub(TdApi.GroupChatInfo info, int size, ImageView target) {
        StubDrawable stub = getStubDrawable(info, size);
        target.setImageDrawable(stub);
        picasso.cancelRequest(target);
    }

    private StubDrawable getStubDrawable(TdApi.GroupChatInfo info, int size) {
        String chars = STUB_AWARE_GROUP_CHAT.needStub(info.groupChat);
        return getStubDrawable(chars, info.groupChat.id, size);
    }

    public void loadAvatarForChat(TdApi.Chat chat, int size, AvatarView avatarView) {
        if (chat.type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chat.type).user;
            loadAvatarForUser(user, size, avatarView);
        } else {
            loadAvatarForGroup(chat, size, avatarView);
        }
    }

    private void loadAvatarForGroup(TdApi.Chat chat, int size, AvatarView avatarView) {
        TdApi.GroupChatInfo info = (TdApi.GroupChatInfo) chat.type;
        TdApi.File file = info.groupChat.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                loadStub(info, size, avatarView);
                return;
            }
        }
        loadPhoto(file, false)
                .resize(size, size)
                .transform(ROUND)
                .placeholder(getStubDrawable(info, size))
                .into(avatarView);
    }

    private final Map<StubKey, StubDrawable> stubs = new HashMap<>();

    public class StubKey {
        final int id;
        final String chars;
        final int size;

        public StubKey(int id, String chars, int size) {
            this.id = id;
            this.chars = chars;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StubKey stubKey = (StubKey) o;

            if (id != stubKey.id) {
                return false;
            }
            if (size != stubKey.size) {
                return false;
            }
            return chars.equals(stubKey.chars);
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + chars.hashCode();
            result = 31 * result + size;
            return result;
        }
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
