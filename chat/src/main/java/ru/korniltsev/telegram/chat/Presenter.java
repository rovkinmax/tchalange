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
    final RXClient rxClient;
    private RxPicasso picasso;

    @Nullable private Observable<MessagesHolder.MessagesAndUsers> request;
    @Nullable private Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    CompositeSubscription subscription;
    //    private Subscription requestSubscription = Subscriptions.unsubscribed();
    private Chat chat;
    private boolean downloadedAll = false;

    @Inject
    public Presenter(RXClient rxClient, RxPicasso picasso) {
        this.rxClient = rxClient;
        this.picasso = picasso;
    }

    MessagesHolder ms = new MessagesHolder();

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        ChatView view = getView();
        chat = Chat.get(view.getContext());

        boolean isGroupChat = isGroupChat(chat.chat);
        if (ms.isEmpty()) {
            ms.add(chat.chat.topMessage, Utils.getUserFromChat(chat.chat));//todo dangerous to npe!!!
            if (request == null) {
                request();
            }
            if (fullChatInfoRequest == null) {
                if (isGroupChat){
                    TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chat.chat.type).groupChat;
                    fullChatInfoRequest = rxClient.getGroupChatInfo(groupChat.id);
                }
            }
        }
        view.loadToolBarImage(chat.chat);
        view.initMenu(isGroupChat);
        view.addMessages(ms.getMs());
        setViewSubtitle();


        if (!isGroupChat){
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
                    request.subscribe((messages) -> {
                        request = null;
                        if (messages.ms.isEmpty()) {
                            downloadedAll = true;
                        } else {
                            ms.add(messages);
                            getView().addMessages(messages);
                        }
                    }));
        }
        if (fullChatInfoRequest != null) {
            subscription.add(
                    fullChatInfoRequest.subscribe(
                            this::updateOnlineStatus));
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

    private void request() {//todo rewrite this shit
        assertNull(request);
        TdApi.Message lastMessage = ms.getLastMessage();
        request = rxClient.getMessages(chat.chat.id, lastMessage.id, 0, Chat.LIMIT)
                .flatMap((ms) -> {
                    //todo do not flatmap on ui thread!
                    TdApi.Message[] messages = ms.messages;
                    users.clear();
                    for (TdApi.Message message : messages) {
                        users.add(message.fromId);
                        if (message.forwardFromId != 0) {
                            users.add(message.forwardFromId);
                        }
                    }
                    List<Observable<TdApi.User>> os = new ArrayList<>();
                    for (Integer uid : users) {
                        os.add(rxClient.getUser(uid));
                    }
                    Observable<List<TdApi.User>> allUsers = Observable.merge(os)
                            .toList();
                    Observable<TdApi.Messages> messagesCopy = Observable.just(ms);
                    return allUsers.zipWith(messagesCopy, (users1, messages1) -> {
                        return new MessagesHolder.MessagesAndUsers(messages1, users1);
                    });
                });
    }

    public void requestNewPortion() {
        request();
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
                        .subscribe(o -> {
                            getView()
                                    .clearAdapter();
                        }));
        ;
    }

    private void leaveGroup() {
        //todo mb progress?!
        //todo config changes
        subscription.add(
                rxClient.sendRXUI(
                        new TdApi.DeleteChatParticipant(chat.chat.id, chat.me.id)
                ).subscribe((o) -> {
                    Flow.get(getView().getContext())
                            .goBack();
                }));
        ;
    }
}
