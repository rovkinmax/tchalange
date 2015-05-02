package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.view.MessagePanel;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import static junit.framework.Assert.assertTrue;

@Singleton
public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener {

    final RXClient client;

    @Nullable private Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    CompositeSubscription subscription;
    private Chat chatPath;
//    private Observable<TdApi.UpdateNewMessage> newMessageUpdates;

    @Inject
    public Presenter(RXClient client) {
        this.client = client;
    }

    MessagesHolder ms;

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        ChatView view = getView();
        chatPath = Chat.get(view.getContext());
        if (ms == null) {
            ms = new MessagesHolder(client, chatPath.chat, new MessagesHolder.AddListener() {
                @Override
                public void historyAdded(MessagesHolder.Portion portion) {
                    getView()
                            .addHistory(portion);
                }

                @Override
                public void newMessageAdded(MessagesHolder.Portion portion) {
                    getView()
                            .addNewMessage(portion);
                }
            });
        }

        boolean isGroupChat = isGroupChat(chatPath.chat);
        if (ms.isEmpty()) {
            if (!ms.isRequestInProgress()) {
                ms.request(chatPath.chat.topMessage);
            }
            if (fullChatInfoRequest == null) {
                if (isGroupChat) {
                    TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chatPath.chat.type).groupChat;
                    fullChatInfoRequest = client.getGroupChatInfo(groupChat.id);
                }
            }
        }
        view.loadToolBarImage(chatPath.chat);
        view.initMenu(isGroupChat);
        view.addHistory(ms.getMs());
        setViewSubtitle();

        if (!isGroupChat) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chatPath.chat.type).user;
            view.setPirvateChatSubtitle(user.status);
        }

        subscribe();
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = chatPath.chat.type;
        if (t instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) t).user;
            getView().setPrivateChatTitle(user);
        } else {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) t).groupChat;
            getView().setGroupChatTitle(groupChat);
        }
    }

    private boolean isGroupChat(TdApi.Chat chat) {
        return chat.type instanceof TdApi.GroupChatInfo;
    }

    @Override
    protected void onExitScope() {
        subscription.unsubscribe();
    }

    private void subscribe() {
        if (subscription != null){
            assertTrue(subscription.isUnsubscribed());
        }
        subscription = new CompositeSubscription();

        subscribeForMessageHistory();
        if (fullChatInfoRequest != null) {
            subscription.add(
                    fullChatInfoRequest.subscribe(
                            new Action1<TdApi.GroupChatFull>() {
                                @Override
                                public void call(TdApi.GroupChatFull groupChatFull) {
                                    updateOnlineStatus(groupChatFull);
                                }
                            }
                    ));
        }
        subscription.add(
                ms.subscribeNewMessages());;
//        subscription.add(
//                newMessageUpdates.subscribe(new Action1<TdApi.UpdateNewMessage>() {
//                    @Override
//                    public void call(TdApi.UpdateNewMessage updateNewMessage) {
//                        ms
//                    }
//                }));
    }

    private void subscribeForMessageHistory() {
        //todo show progressBar
        if (ms.isRequestInProgress()) {
            subscription.add(
                    ms.subscribe());
        }
    }

    private void updateOnlineStatus(TdApi.GroupChatFull info) {
        int online = 0;
        for (TdApi.ChatParticipant p : info.participants) {
            if (p.user.status instanceof TdApi.UserStatusOnline) {
                online++;
            }
        }
        getView().setwGroupChatSubtitle(info.participants.length, online);
    }

    public void requestNewPortion() {
        ms.request(null);
        subscribeForMessageHistory();
    }

    public void listScrolledToEnd() {
        if (ms.isDownloadedAll()) {
            return;
        }
        if (ms.isRequestInProgress()) {
            return;
        }
        requestNewPortion();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (R.id.menu_leave_group == menuItem.getItemId()) {
            leaveGroup();
            return true;
        } else if (R.id.menu_clear_history == menuItem.getItemId()) {
            clearHistory();
            return true;
        }
        return false;
    }

    private void clearHistory() {
        //todo mb progress?!
        //todo config changes
        subscription.add(
                client.sendRXUI(
                        new TdApi.DeleteChatHistory(chatPath.chat.id))
                        .subscribe(new Action1<TdApi.TLObject>() {
                            @Override
                            public void call(TdApi.TLObject o) {
                                getView()
                                        .clearAdapter();
                            }
                        }));
        ;
    }

    private void leaveGroup() {
        //todo mb progress?!
        //todo config changes
        subscription.add(
                client.sendRXUI(
                        new TdApi.DeleteChatParticipant(chatPath.chat.id, chatPath.me.id)
                ).subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject o) {
                        Flow.get(getView().getContext())
                                .goBack();
                    }
                }));
        ;
    }

    @Override
    public void sendText(String text) {
        TdApi.InputMessageText content = new TdApi.InputMessageText(text);
        client.sendSilently(new TdApi.SendMessage(chatPath.chat.id, content));
    }
}
