package ru.korniltsev.telegram.chat_list;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import flow.Flow;
import junit.framework.Assert;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.android.content.ContentObservable;
import rx.android.view.OnClickEvent;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.net.NetworkInfo.State.CONNECTED;

@WithModule(ChatList.Module.class)
public class ChatList extends BasePath implements Serializable {

    @dagger.Module(injects = ChatListView.class, addsTo = RootModule.class)
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<ChatListView> {
        public static final int LIMIT = 10;
        private final RXClient client;
        private RXAuthState authState;
        @Nullable private Observable<TdApi.Chats> chatsRequest;
        @Nullable Observable<TdApi.User> meRequest;
        private TdApi.User me;
        private Observable<Intent> networkState;

        private CompositeSubscription subscription;

        //strange flags
        boolean downloadedAll = false;
        boolean atLeastOneResponseReturned = false;

        @Inject
        public Presenter(RXClient client, RXAuthState authState) {
            this.client = client;
            this.authState = authState;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            if (chatsRequest == null) {
                if (!atLeastOneResponseReturned) {
                    requestChats();
                    meRequest = chatsRequest.flatMap(chats -> client.getMe());
                } else {
                    //wait for scroll
                }
            }

            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            networkState = ContentObservable.fromBroadcast(getView().getContext(), filter);

            getView()
                    .addChats(chatsHolder);
            subscribe();
        }

        final List<TdApi.Chat> chatsHolder = new ArrayList<>();

        private void requestChats() {
            Assert.assertNull(chatsRequest);
            chatsRequest = client.getChats(chatsHolder.size(), LIMIT);
        }

        private void subscribe() {
            subscription = new CompositeSubscription();
            subscribeChats();

            if (meRequest != null) {
                subscription.add(
                        meRequest.subscribe(user -> {
                            getView()
                                    .showMe(user);
                            me = user;
                        }));
            }
            subscription.add(
                    networkState.subscribe(i -> {
                        NetworkInfo networkInfo = i.getExtras()
                                .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
                        NetworkInfo.State state = networkInfo.getState();
                        getView()
                                .updateNetworkStatus(state == CONNECTED);
                    }));
        }

        private void subscribeChats() {
            if (chatsRequest != null) {
                subscription.add(
                        chatsRequest.subscribe(chats -> {
                            chatsRequest = null;
                            atLeastOneResponseReturned = true;
                            if (chats.chats.length == 0) {
                                downloadedAll = true;
                                return;
                            }
                            List<TdApi.Chat> ts = Arrays.asList(chats.chats);
                            chatsHolder.addAll(ts);
                            getView()
                                    .addChats(ts);
                        }));
            }
        }

        public void openChat(TdApi.Chat chat) {
            if (supportedChats(chat)){
                Flow.get(getView())
                        .set(new Chat(chat, me)); //todo possible npe
            } else {
                //todo
            }
        }

        private static boolean supportedChats(TdApi.Chat chat) {
            return chat.type instanceof TdApi.PrivateChatInfo
                    || chat.type instanceof TdApi.GroupChatInfo;
        }

        public void logout(OnClickEvent e) {
            authState.logout();
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            subscription.unsubscribe();
        }

        public void listScrolledToEnd() {
            if (chatsRequest != null) return;
            if (downloadedAll) return;
            requestNewPortion();
        }

        private void requestNewPortion() {
            requestChats();
            subscribeChats();

        }
    }

    @Override
    public int getRootLayout() {
        return R.layout.fragment_chat_list;
    }
}
