package ru.korniltsev.telegram.chat_list;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.ChatDB;
import rx.Observable;
import rx.android.content.ContentObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.List;

import static android.net.NetworkInfo.State.CONNECTED;
import static junit.framework.Assert.assertTrue;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@WithModule(ChatList.Module.class)
public class ChatList extends BasePath implements Serializable {

    @dagger.Module(injects = ChatListView.class, addsTo = RootModule.class)
    public static class Module {

    }

    @Singleton
    public static class Presenter extends ViewPresenter<ChatListView> {
        private RXClient client;
        private final Emoji emoji;
        private RXAuthState authState;
        private ChatDB chatDB;
        final Observable<TdApi.User> meRequest;
        private TdApi.User me;
        private Observable<TdApi.UpdateOption> networkState;

        private CompositeSubscription subscription;

        //strange flags
        //        boolean atLeastOneResponseReturned = false;

        @Inject
        public Presenter(RXClient client, Emoji emoji, RXAuthState authState, ChatDB chatDB) {
            this.client = client;
            this.emoji = emoji;
            this.authState = authState;
            this.chatDB = chatDB;
            checkTlObjectIsSerializable();
            meRequest = client.getMe();
            networkState = client.getConnectedState()
                .observeOn(mainThread());
        }

        private void checkTlObjectIsSerializable() {
            //do not forget to implement seralizable after libtd update
            //noinspection ConstantConditions
            assertTrue(new TdApi.UpdateFile() instanceof Serializable);
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);

            if (!chatDB.isRequestInProgress()) {
                if (!chatDB.isAtLeastOneResponseReturned()) {
                    requestChats();
                } //else wait for scroll
            }

            //todo use libtd
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            ContentObservable.fromBroadcast(getView().getContext(), filter);

            getView()
                    .setData(chatDB.getAllChats());

            subscribe();
        }

        private void requestChats() {
            chatDB.requestPortion();
        }

        private void subscribe() {
            subscription = new CompositeSubscription();
            subscription.add(
                    chatDB.chatList().subscribe(new Action1<List<TdApi.Chat>>() {
                        @Override
                        public void call(List<TdApi.Chat> chats) {
                            getView()
                                    .setData(chats);
                        }
                    }));

            subscription.add(
                    meRequest.subscribe(new Action1<TdApi.User>() {
                        @Override
                        public void call(TdApi.User user) {
                            Presenter.this.getView()
                                    .showMe(user);
                            me = user;
                        }
                    }));
            subscription.add(
                    networkState.subscribe(new Action1<TdApi.UpdateOption>() {
                        @Override
                        public void call(TdApi.UpdateOption o) {
                            TdApi.OptionString b = (TdApi.OptionString) o.value;
                            boolean connected ;
                            switch (b.value){
                                case "Waiting for network":
                                case "Connecting":
                                    connected = false;
                                 break;
                                default:
                                    connected = true;
                                    break;
                            }
                            Presenter.this.getView()
                                    .updateNetworkStatus(connected);
                        }
                    }));
            subscription.add(emoji.pageLoaded()
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            getView()
                                    .invalidate();
                        }
                    }));
        }

        public void openChat(TdApi.Chat chat) {
            if (supportedChats(chat)) {
                Flow.get(getView())
                        .set(new Chat(chat, me));
            } //else do nothing
        }

        private static boolean supportedChats(TdApi.Chat chat) {
            return chat.type instanceof TdApi.PrivateChatInfo
                    || chat.type instanceof TdApi.GroupChatInfo;
        }

        public void logout() {
            authState.logout();
        }

        @Override
        public void dropView(ChatListView view) {
            super.dropView(view);
            subscription.unsubscribe();
        }

        public void listScrolledToEnd() {
            if (chatDB.isRequestInProgress()) {
                return;
            }
            if (chatDB.isDownloadedAllChats()) {
                return;
            }
            chatDB.requestPortion();
        }
    }

    @Override
    public int getRootLayout() {
        return R.layout.fragment_chat_list;
    }
}
