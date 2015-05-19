package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.ChatDB;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener {

    private final Chat path;
    private final RXClient client;
    private final RxChat rxChat;
    private final NotificationManager nm;

    private final Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    private final boolean isGroupChat;
    private CompositeSubscription subscription;
    @Nullable private volatile TdApi.GroupChatFull mGroupChatFull;

    public Chat getPath() {
        return path;
    }

    @Inject
    public Presenter(Chat c, RXClient client, ChatDB chatDB, NotificationManager nm) {
        path = c;
        this.client = client;
        this.nm = nm;
        rxChat = chatDB.getRxChat(path.chat.id);

        if (path.chat.type instanceof TdApi.GroupChatInfo) {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) path.chat.type).groupChat;
            fullChatInfoRequest = client.getGroupChatInfo(groupChat.id)
                    .observeOn(mainThread());
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
        view.initMenu(isGroupChat, nm.isMuted(path.chat));
        setViewSubtitle();

        List<RxChat.ChatListItem> messages = rxChat.getMessages();
        if (!messages.isEmpty()
                && !rxChat.isRequestInProgress()) {
            rxChat.updateCurrentMessageList();
        }
        getView().updateData(rxChat);
        subscribe();
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = path.chat.type;
        if (t instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) t).user;
            setViewTitle(user);
        } else {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) t).groupChat;
            getView().setGroupChatTitle(groupChat);
        }
    }

    private void setViewTitle(TdApi.User user) {
        getView().setPrivateChatTitle(user);
        getView().setPirvateChatSubtitle(user.status);
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
                rxChat.messageList()
                        .subscribe(new Action1<List<RxChat.ChatListItem>>() {
                            @Override
                            public void call(List<RxChat.ChatListItem> messages) {
                                getView()
                                        .getAdapter()
                                        .setData(messages);
                            }
                        }));

        subscription.add(
                rxChat.newMessage()
                        .subscribe(new Action1<List<RxChat.ChatListItem>>() {
                                       @Override
                                       public void call(List<RxChat.ChatListItem> chatListItems) {
                                           getView()
                                                   .addNewMessage(chatListItems);
                                           rxChat.hackToReadTheMessage(chatListItems);
                                       }
                                   }
                        ));

        requestUpdateOnlineStatus();

        subscription.add(
                nm.updatesForChat(path.chat)
                        .subscribe(new Action1<TdApi.NotificationSettings>() {
                                       @Override
                                       public void call(TdApi.NotificationSettings s) {
                                           getView().initMenu(isGroupChat, nm.isMuted(s));
                                       }
                                   }
                        ));

        subscription.add(
                updateReadOutbox()
                        .subscribe(new Action1<TdApi.UpdateChatReadOutbox>() {
                            @Override
                            public void call(TdApi.UpdateChatReadOutbox upd) {
                                getView()
                                        .getAdapter()
                                        .setLastReadOutbox(upd.lastRead);
                            }
                        }));

        subscription.add(
                rxChat.holder.getMessageIdsUpdates(path.chat.id)
                        .subscribe(new Action1<TdApi.UpdateMessageId>() {
                            @Override
                            public void call(TdApi.UpdateMessageId updateMessageId) {
                                getView()
                                        .getAdapter()
                                        .notifyDataSetChanged();
                            }
                        })
        );

        subscription.add(usersStatus()
                .subscribe(new Action1<TdApi.UpdateUserStatus>() {
                    @Override
                    public void call(TdApi.UpdateUserStatus updateUserStatus) {
                        requestUpdateOnlineStatus();
                    }
                }));

        subscription.add(
                updatesChatsParticipantCount()
                        .subscribe(new Action1<TdApi.UpdateChatParticipantsCount>() {
                            @Override
                            public void call(TdApi.UpdateChatParticipantsCount updateChatParticipantsCount) {
                                requestUpdateOnlineStatus();
                            }
                        }));
    }

    private Observable<TdApi.UpdateChatParticipantsCount> updatesChatsParticipantCount() {
        return client.chatParticipantCount().filter(new Func1<TdApi.UpdateChatParticipantsCount, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatParticipantsCount upd) {
                return upd.chatId == path.chat.id;
            }
        }).observeOn(mainThread());
    }

    private Observable<TdApi.UpdateUserStatus> usersStatus() {
        return client.usersStatus()

                .filter(new Func1<TdApi.UpdateUserStatus, Boolean>() {
                    @Override
                    public Boolean call(TdApi.UpdateUserStatus updateUserStatus) {
                        if (isGroupChat) {
                            TdApi.GroupChatFull mGroupChatFullCopy = Presenter.this.mGroupChatFull;
                            if (mGroupChatFullCopy == null) {
                                return true;
                            }
                            for (TdApi.ChatParticipant p : mGroupChatFullCopy.participants) {
                                if (p.user.id == updateUserStatus.userId) {
                                    return true;
                                }
                            }
                            return false;
                        } else {
                            return getChatUserId() == updateUserStatus.userId;
                        }
                    }
                }).observeOn(mainThread());
    }

    private int getChatUserId() {
        if (isGroupChat) {
            throw new IllegalStateException();
        }
        TdApi.PrivateChatInfo type = (TdApi.PrivateChatInfo) path.chat.type;
        return type.user.id;
    }

    private void requestUpdateOnlineStatus() {
        checkMainThread();
        if (isGroupChat) {
            subscription.add(
                    fullChatInfoRequest.subscribe(
                            new Action1<TdApi.GroupChatFull>() {
                                @Override
                                public void call(TdApi.GroupChatFull groupChatFull) {
                                    mGroupChatFull = groupChatFull;
                                    updateGroupChatOnlineStatus(groupChatFull);
                                }
                            }
                    ));
        } else {
            subscription.add(
                    getUser().subscribe(new Action1<TdApi.User>() {
                        @Override
                        public void call(TdApi.User user) {
                            setViewTitle(user);
                        }
                    }));
        }
    }

    private Observable<TdApi.User> getUser() {
        return client.getUser(getChatUserId()).observeOn(mainThread());
    }

    private Observable<TdApi.UpdateChatReadOutbox> updateReadOutbox() {
        return client.updateChatReadOutbox().filter(new Func1<TdApi.UpdateChatReadOutbox, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatReadOutbox updateChatReadOutbox) {
                return updateChatReadOutbox.chatId == path.chat.id;
            }
        }).observeOn(mainThread());
    }

    private void updateGroupChatOnlineStatus(TdApi.GroupChatFull info) {
        int online = 0;
        for (TdApi.ChatParticipant p : info.participants) {
            if (p.user.status instanceof TdApi.UserStatusOnline) {
                online++;
            }
        }
        getView().setwGroupChatSubtitle(info.participants.length, online);
    }

    public void requestNewPortion() {
        rxChat.requestNewPotion();
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
        int id = menuItem.getItemId();
        if (R.id.menu_leave_group == id) {
            leaveGroup();
            return true;
        } else if (R.id.menu_clear_history == id) {
            clearHistory();
            return true;
        } else if (R.id.menu_mute == id) {
            nm.mute(path.chat);
            getView().initMenu(isGroupChat, true);
        } else if (R.id.menu_unmute == id) {
            nm.unmute(path.chat);
            getView().initMenu(isGroupChat, false);
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
