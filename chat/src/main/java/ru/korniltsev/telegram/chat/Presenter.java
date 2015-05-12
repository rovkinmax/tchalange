package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.ChatDB;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static junit.framework.Assert.assertTrue;

@Singleton
public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener {

    private final Chat path;
    private final RXClient client;
    private final RxChat rxChat;

    private final Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    private final boolean isGroupChat;
    private CompositeSubscription subscription;

    @Inject
    public Presenter(Chat c, RXClient client, ChatDB chatDB) {
        path = c;
        this.client = client;
        rxChat = chatDB.getRxChat(path.chat.id);

        if (path.chat.type instanceof TdApi.GroupChatInfo) {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) path.chat.type).groupChat;
            fullChatInfoRequest = client.getGroupChatInfo(groupChat.id);
            isGroupChat = true;
        } else {
            fullChatInfoRequest = Observable.empty();
            isGroupChat = false;
        }
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        ChatView view = getView();

        if (!rxChat.atLeastOneRequestCompleted()) {
            if (!rxChat.isRequestInProgress()) {
                rxChat.request2(path.chat.topMessage, path.chat.topMessage);
                //todo move first request to constructor
            }
        }
        view.loadToolBarImage(path.chat);
        view.initMenu(isGroupChat);
        setViewSubtitle();


        getView().updateData(rxChat);
        subscribe();
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = path.chat.type;
        if (t instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) t).user;
            getView().setPrivateChatTitle(user);
            getView().setPirvateChatSubtitle(user.status);
        } else {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) t).groupChat;
            getView().setGroupChatTitle(groupChat);
        }
    }


    @Override
    public void dropView(ChatView view) {
        super.dropView(view);
        subscription.unsubscribe();
    }

    private void subscribe() {
        if (subscription != null) {
            assertTrue(subscription.isUnsubscribed());
        }
        subscription = new CompositeSubscription();

        //todo show progressBar
        subscription.add(
                rxChat.messageList().subscribe(new Action1<List<TdApi.Message>>() {
                    @Override
                    public void call(List<TdApi.Message> messages) {
                        getView()
                                .getAdapter()
                                .setData(messages);
                    }
                }));

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
        TdApi.Message lastMessage = getView()
                .getAdapter()
                .getLast();
        rxChat.request2(lastMessage, null);
    }

    public void listScrolledToEnd() {
        if (rxChat.isDownloadedAll()) {
            return;
        }
        if (rxChat.isRequestInProgress()) {
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
        rxChat.deleteHistory();
    }

    private void leaveGroup() {
//        rxChat.
        //todo mb progress?!
        //todo config changes
        subscription.add(
                client.sendCachedRXUI(
                        new TdApi.DeleteChatParticipant(path.chat.id, path.me.id)
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
        rxChat.sendMessage(text);
    }

}
