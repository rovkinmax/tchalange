package ru.korniltsev.telegram.chat_list;

import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.android.content.ContentObservable;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatListPresenter extends ViewPresenter<ChatListView> {
    private final ChatList cl;
    private final RXClient client;
    private final Emoji emoji;
    private final RXAuthState authState;
    private final ChatDB chatDB;

    final Observable<TdApi.User> meRequest;
    private TdApi.User me;
    private Observable<TdApi.UpdateOption> networkState;

    private CompositeSubscription subscription;

    //strange flags
    //        boolean atLeastOneResponseReturned = false;

    @Inject
    public ChatListPresenter(ChatList cl, RXClient client, Emoji emoji, RXAuthState authState, ChatDB chatDB) {
        this.cl = cl;
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

        List<TdApi.Chat> allChats = chatDB.getAllChats();
        getView().setData(allChats);
        if (!allChats.isEmpty()
                && !chatDB.isRequestInProgress()){
            chatDB.updateCurrentChatList();
        }

        subscribe();
    }

    private void requestChats() {
        chatDB.requestPortion();
    }

    private void subscribe() {
        subscription = new CompositeSubscription();
        subscription.add(
                chatDB.chatList().subscribe(new ObserverAdapter<List<TdApi.Chat>>() {
                    @Override
                    public void onNext(List<TdApi.Chat> chats) {
                        getView()
                                .setData(chats);
                    }
                }));

        subscription.add(
                meRequest.subscribe(new ObserverAdapter<TdApi.User>() {
                    @Override
                    public void onNext(TdApi.User user) {
                        ChatListPresenter.this.getView()
                                .showMe(user);
                        me = user;
                    }
                }));
        subscription.add(
                networkState.subscribe(new ObserverAdapter<TdApi.UpdateOption>() {
                    @Override
                    public void onNext(TdApi.UpdateOption o) {
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
                        ChatListPresenter.this.getView()
                                .updateNetworkStatus(connected);
                    }
                }));
        subscription.add(emoji.pageLoaded()
                .subscribe(new ObserverAdapter<Bitmap>() {
                    @Override
                    public void onNext(Bitmap response) {
                        getView()
                                .invalidate();
                    }
                }));
    }

    public void openChat(TdApi.Chat chat) {
        if (supportedChats(chat)) {
//                chat.unreadCount = 0;//todo why here?!?!?!
            Flow.get(getView())
                    .set(new Chat(chat, me));
        } //else do nothing
    }

    private static boolean supportedChats(TdApi.Chat chat) {
        return chat.type instanceof TdApi.PrivateChatInfo
                || chat.type instanceof TdApi.GroupChatInfo;
    }

    public void logout() {
        client.logout();
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

    public ChatList getCl() {
        return cl;
    }
}
