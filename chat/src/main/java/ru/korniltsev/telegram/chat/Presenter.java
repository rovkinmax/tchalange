package ru.korniltsev.telegram.chat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxPicasso;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertNull;

@Singleton
public class Presenter extends ViewPresenter<ChatView> implements Toolbar.OnMenuItemClickListener {
    public static final Func2<List<TdApi.User>, List<TdApi.Message>, MessagesHolder.MessagesAndUsers> ZIPPER = new Func2<List<TdApi.User>, List<TdApi.Message>, MessagesHolder.MessagesAndUsers>() {
        @Override
        public MessagesHolder.MessagesAndUsers call(List<TdApi.User> users, List<TdApi.Message> messages) {
            return new MessagesHolder.MessagesAndUsers(messages, users);
        }
    };
    //    public static final ListMessagesMessagesAndUsersFunc2 ZIPPER = new ListMessagesMessagesAndUsersFunc2();
    final RXClient rxClient;

    @Nullable private Observable<MessagesHolder.MessagesAndUsers> request;
    @Nullable private Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    CompositeSubscription subscription;
    private Chat chat;
    private boolean downloadedAll = false;

    @Inject
    public Presenter(RXClient rxClient) {
        this.rxClient = rxClient;
    }

    MessagesHolder ms = new MessagesHolder();

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        ChatView view = getView();
        chat = Chat.get(view.getContext());

        boolean isGroupChat = isGroupChat(chat.chat);
        if (ms.isEmpty()) {
            if (request == null) {
                request(chat.chat.topMessage);
            }
            if (fullChatInfoRequest == null) {
                if (isGroupChat) {
                    TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chat.chat.type).groupChat;
                    fullChatInfoRequest = rxClient.getGroupChatInfo(groupChat.id);
                }
            }
        }
        view.loadToolBarImage(chat.chat);
        view.initMenu(isGroupChat);
        view.addMessages(ms.getMs());
        setViewSubtitle();

        if (!isGroupChat) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chat.chat.type).user;
            view.setPirvateChatSubtitle(user.status);
        }

        subscribe();
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = chat.chat.type;
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
        subscription = new CompositeSubscription();
        if (request != null) {
            //todo show progressBar
            subscription.add(
                    request.subscribe(new Action1<MessagesHolder.MessagesAndUsers>() {
                        @Override
                        public void call(MessagesHolder.MessagesAndUsers portion) {
                            request = null;

                            if (portion.ms.isEmpty()) {
                                downloadedAll = true;
                            } else {
                                ms.add(portion);
                                Presenter.this.getView().addMessages(portion);
                            }
                        }
                    }));
        }
        if (fullChatInfoRequest != null) {
            subscription.add(
                    fullChatInfoRequest.subscribe(new Action1<TdApi.GroupChatFull>() {
                                                      @Override
                                                      public void call(TdApi.GroupChatFull groupChatFull) {
                                                          updateOnlineStatus(groupChatFull);
                                                      }
                                                  }
                    ));
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

    private final Set<Integer> users = new HashSet<>();//todo object allocs!

    private void request(@Nullable final TdApi.Message initMessage) {//todo rewrite this shit
        assertNull(request);
        TdApi.Message lastMessage;
        if (initMessage != null){
            lastMessage = initMessage;
        } else {
            lastMessage = ms.getLastMessage();

        }
        request = rxClient.getMessages(chat.chat.id, lastMessage.id, 0, Chat.LIMIT)
                .flatMap(new Func1<TdApi.Messages, Observable<? extends MessagesHolder.MessagesAndUsers>>() {
                    @Override
                    public Observable<? extends MessagesHolder.MessagesAndUsers> call(TdApi.Messages portion) {
                        //todo do not flatmap on ui thread!
                        TdApi.Message[] messages = portion.messages;
                        users.clear();
                        for (TdApi.Message message : messages) {
                            getUIDs(message);
                        }
                        if (ms.isEmpty()){
                            getUIDs(chat.chat.topMessage);
                        }
                        List<Observable<TdApi.User>> os = new ArrayList<>();
                        for (Integer uid : users) {
                            os.add(rxClient.getUser(uid));
                        }
                        Observable<List<TdApi.User>> allUsers = Observable.merge(os)
                                .toList();
                        final List<TdApi.Message> messageList= new ArrayList<>();
                        if (initMessage != null) {
                            messageList.add(initMessage);
                        }
                        for (TdApi.Message m : portion.messages) {
                            messageList.add(m);
                        }
                        Observable<List<TdApi.Message>> messagesCopy = Observable.just(messageList);
                        return allUsers.zipWith(messagesCopy, ZIPPER);
                    }
                });
    }

    private void getUIDs(TdApi.Message message) {
        users.add(message.fromId);
        if (message.forwardFromId != 0) {
            users.add(message.forwardFromId);
        }
    }

    public void requestNewPortion() {
        request(null);
        subscribe();
    }

    public void listScrolledToEnd() {
        if (downloadedAll) {
            return;
        }
        if (request != null) {
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
                rxClient.sendRXUI(
                        new TdApi.DeleteChatHistory(chat.chat.id))
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
                rxClient.sendRXUI(
                        new TdApi.DeleteChatParticipant(chat.chat.id, chat.me.id)
                ).subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject o) {
                        Flow.get(getView().getContext())
                                .goBack();
                    }
                }));
        ;
    }


}
