package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

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

    private  MessagesHolder ms;
    private Adapter.Portion savedDataFromAdapter;


    @Override
    protected void onLoad(Bundle savedInstanceState) {
        ChatView view = getView();
        chatPath = Chat.get(view.getContext());
        if (ms == null){
            ms = new MessagesHolder(client, chatPath.chat, new MessagesHolder.AddListener() {
                @Override
                public void historyAdded(Adapter.Portion portion) {
                    getView()
                            .addHistory(portion);
                }

                @Override
                public void newMessageAdded(Adapter.Portion portion) {
                    getView()
                            .addNewMessage(portion);
                }

                @Override
                public boolean hasUser(Integer id) {
                    Map<Integer, TdApi.User> users;
                    ChatView view = getView();
                    if (view == null) {
                        users = savedDataFromAdapter.us;
                    } else {
                        users = view.getAdapter()
                                .getUsers();
                    }
                    return users.containsKey(id);
                }
            });
        }


        boolean isGroupChat = isGroupChat(chatPath.chat);
        if (!ms.atLeastOneRequestCompleted()) {
            if (!ms.isRequestInProgress()) {
                ms.request2(chatPath.chat.topMessage, chatPath.chat.topMessage);
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
        if (savedDataFromAdapter != null){
            view.addHistory(savedDataFromAdapter);
        }
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
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        savedDataFromAdapter = getView()
                .getAdapter()
                .getPortion();
    }

    @Override
    public void dropView(ChatView view) {
        super.dropView(view);
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
                ms.subscribeNewMessages());

        subscription.add(client.messageIdsUpdates(chatPath.chat.id)
                .subscribe(new Action1<TdApi.UpdateMessageId>() {
                    @Override
                    public void call(TdApi.UpdateMessageId upd) {
//                        ms.updateMessageId(upd);
                        getView()
                                .getAdapter()
                                .updateMessageId(upd);

                    }
                }));
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
        TdApi.Message lastMessage = getView()
                .getAdapter()
                .getLast();
        ms.request2(lastMessage, null);
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
        client.sendRXUI(new TdApi.SendMessage(chatPath.chat.id, content))
        //todo manage subscription
        .subscribe(new Action1<TdApi.TLObject>() {
            @Override
            public void call(TdApi.TLObject tlObject) {
                System.out.println(tlObject);
                TdApi.Message msg = (TdApi.Message) tlObject;
                Adapter.Portion p = new Adapter.Portion(msg);
                //                ms.add(p, false);
                getView().addNewMessage(p);
            }
        });

    }

//    public void saveLoadedPortion(Adapter.Portion portion) {
//        savedDataFromAdapter = portion;
//    }
}
