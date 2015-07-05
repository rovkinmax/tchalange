package ru.korniltsev.telegram.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.ChatDB;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener,
        AttachPanelPopup.Callback

{

    public static final int REQUEST_CHOOS_FROM_GALLERY = 1;
    public static final int REQUEST_TAKE_PHOTO = 2;
    private final Chat path;
    private final RXClient client;
    private final RxChat rxChat;
    private final NotificationManager nm;

    private final Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    private final boolean isGroupChat;
    private final TdApi.Chat chat;
    private CompositeSubscription subscription;
    @Nullable private volatile TdApi.GroupChatFull mGroupChatFull;

    public Chat getPath() {
        return path;
    }

    private final ActivityOwner owner;
    private final Stickers stickers;
    @Inject
    public Presenter(Chat c, RXClient client, ChatDB chatDB, NotificationManager nm, ActivityOwner owner, Stickers stickers) {
        path = c;
        this.client = client;
        this.nm = nm;
        this.owner = owner;
        this.stickers = stickers;
        this.chat = path.chat;
        rxChat = chatDB.getRxChat(chat.id);

        if (chat.type instanceof TdApi.GroupChatInfo) {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chat.type).groupChat;
            fullChatInfoRequest = client.getGroupChatInfo(groupChat.id)
                    .observeOn(mainThread());
            isGroupChat = true;
        } else {
            fullChatInfoRequest = Observable.empty();
            isGroupChat = false;
        }

        //todo if has unread messages load from it
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        if (path.firstLoad) {
            path.firstLoad = false;
            rxChat.clear();
            if (chat.unreadCount == 0){
                rxChat.initialRequest(chat);
            } else {
                rxChat.requestUntilLastUnread(chat);
            }

        }


        ChatView view = getView();

        view.loadToolBarImage(this.chat);
        view.initMenu(isGroupChat, nm.isMuted(this.chat));
        setViewSubtitle();

        getView().initList(rxChat);
        subscribe();
        if (isGroupChat) {
            TdApi.GroupChatInfo g = (TdApi.GroupChatInfo) this.chat.type;
            showMessagePanel(g.groupChat);
        }
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = chat.type;
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
        getView().setPirvateChatSubtitle(rxChat.holder.getUserStatus(user));
    }

    @Override
    public void dropView(ChatView view) {
        super.dropView(view);
        subscription.unsubscribe();
        //        Utils.hideKeyboard(view);
    }

    private void subscribe() {
        if (subscription != null) {
            assertTrue(subscription.isUnsubscribed());
        }
        subscription = new CompositeSubscription();

        subscription.add(
                rxChat.history()
                        .subscribe(new ObserverAdapter<RxChat.HistoryResponse>(){
                            @Override
                            public void onNext(RxChat.HistoryResponse history) {
                                //todo if unread messages
                                getView().addHistory(chat, history);
                            }
                        })
        );
//        subscription.add(
//                rxChat.messageList()
//                        .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
//                            @Override
//                            public void onNext(List<TdApi.Message> messages) {
//                                getView()
//                                        .setMessages(messages);
//
//                            }
//                        }));

        subscription.add(
                rxChat.getDeletedMessagesSubject()
                        .subscribe(new ObserverAdapter<RxChat.DeletedMessages>(){
                            @Override
                            public void onNext(RxChat.DeletedMessages response) {
                                getView().deleteMessages(response);
                            }
                        })
        );
        subscription.add(
                rxChat.getNewMessage()
                        .subscribe(new ObserverAdapter<TdApi.Message>() {
                                       @Override
                                       public void onNext(TdApi.Message chatListItems) {
                                           getView()
                                                   .addNewMessage(chatListItems);
                                           rxChat.hackToReadTheMessage(chatListItems);
                                       }
                                   }
                        ));

        requestUpdateOnlineStatus();

        subscription.add(
                nm.updatesForChat(chat)
                        .subscribe(new ObserverAdapter<TdApi.NotificationSettings>() {
                                       @Override
                                       public void onNext(TdApi.NotificationSettings s) {
                                           getView().initMenu(isGroupChat, nm.isMuted(s));
                                       }
                                   }
                        ));

        subscription.add(
                updateReadOutbox()
                        .subscribe(new ObserverAdapter<TdApi.UpdateChatReadOutbox>() {
                            @Override
                            public void onNext(TdApi.UpdateChatReadOutbox response) {
                                getView()
                                        .getAdapter()
                                        .setLastReadOutbox(response.lastRead);
                            }
                        }));

        subscription.add(
                rxChat.holder.getMessageIdsUpdates(chat.id)
                        .subscribe(new ObserverAdapter<TdApi.UpdateMessageId>() {
                            @Override
                            public void onNext(TdApi.UpdateMessageId response) {
                                getView()
                                        .getAdapter()
                                        .notifyDataSetChanged();
                            }
                        })
        );

        subscription.add(usersStatus()
                .subscribe(new ObserverAdapter<TdApi.UpdateUserStatus>() {
                    @Override
                    public void onNext(TdApi.UpdateUserStatus response) {
                        requestUpdateOnlineStatus();
                    }
                }));

        subscription.add(
                updatesChatsParticipantCount()
                        .subscribe(new ObserverAdapter<TdApi.UpdateChatParticipantsCount>() {
                            @Override
                            public void onNext(TdApi.UpdateChatParticipantsCount response) {
                                requestUpdateOnlineStatus();
                            }
                        }));

        subscription.add(
                rxChat.getMessageChanged()
                .subscribe(new ObserverAdapter<TdApi.Message>(){
                    @Override
                    public void onNext(TdApi.Message response) {
                        getView().messageChanged(response);
                    }
                })
        );

        subscription.add(
                owner.activityResult()
                        .subscribe(new ObserverAdapter<ActivityResult>() {
                            @Override
                            public void onNext(ActivityResult response) {
                                onActivityResult(response.request, response.result, response.data);
                            }
                        }));
    }

    private Observable<TdApi.UpdateChatParticipantsCount> updatesChatsParticipantCount() {
        return client.chatParticipantCount().filter(new Func1<TdApi.UpdateChatParticipantsCount, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatParticipantsCount upd) {
                return upd.chatId == chat.id;
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
        TdApi.PrivateChatInfo type = (TdApi.PrivateChatInfo) chat.type;
        return type.user.id;
    }

    private void requestUpdateOnlineStatus() {
        checkMainThread();
        if (isGroupChat) {
            subscription.add(
                    fullChatInfoRequest.subscribe(
                            new ObserverAdapter<TdApi.GroupChatFull>() {
                                @Override
                                public void onNext(TdApi.GroupChatFull groupChatFull) {
                                    mGroupChatFull = groupChatFull;
                                    showMessagePanel(mGroupChatFull.groupChat);
                                    updateGroupChatOnlineStatus(groupChatFull);
                                }

                                @Override
                                public void onError(Throwable th) {
                                    //todo
                                }
                            }
                    ));
        } else {
            subscription.add(
                    getUser().subscribe(new ObserverAdapter<TdApi.User>() {
                        @Override
                        public void onNext(TdApi.User user) {
                            setViewTitle(user);
                        }
                    }));
        }
    }

    private void showMessagePanel(TdApi.GroupChat groupChat) {
        //        if (g.groupChat.left){
        getView().showMessagePanel(groupChat.left);
        //        }
    }

    private Observable<TdApi.User> getUser() {
        return client.getUser(getChatUserId()).observeOn(mainThread());
    }

    private Observable<TdApi.UpdateChatReadOutbox> updateReadOutbox() {
        return client.updateChatReadOutbox().filter(new Func1<TdApi.UpdateChatReadOutbox, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatReadOutbox updateChatReadOutbox) {
                return updateChatReadOutbox.chatId == chat.id;
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



    public void listScrolledToEnd() {
        if (rxChat.isDownloadedAll()) {
            return;
        }
        if (rxChat.isRequestInProgress()) {
            return;
        }
        rxChat.requestNewPotion();
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
            nm.mute(chat);
            getView().initMenu(isGroupChat, true);
        } else if (R.id.menu_unmute == id) {
            nm.unmute(chat);
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
                        new TdApi.DeleteChatParticipant(chat.id, path.me.id)
                ).subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        Flow.get(getView().getContext())
                                .goBack();
                    }
                }));
        ;
    }

    @Override
    public void sendText(final String text) {
        getView().scrollToBottom();
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                rxChat.sendMessage(text);
            }
        }, 32);
    }

    public void sendSticker(final String stickerFilePath, TdApi.Sticker sticker) {
        stickers.map(stickerFilePath, sticker);
        getView().scrollToBottom();
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                rxChat.sendSticker(stickerFilePath);
            }
        }, 32);
    }

    @Override
    public void sendImages(List<String> selecteImages) {
        for (String img : selecteImages) {
            rxChat.sendImage(img);
        }
        getView()
                .hideAttachPannel();
    }

    @Override
    public void chooseFromGallery() {
        String title = getView().getContext().getString(R.string.select_picture);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        owner.expose()
                .startActivityForResult(Intent.createChooser(intent, title), REQUEST_CHOOS_FROM_GALLERY);
    }

    @Override
    public void takePhoto() {
        File f = getTmpFileForCamera();
        f.delete();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        owner.expose()
                .startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    @NonNull
    private File getTmpFileForCamera() {
        return new File(Environment.getExternalStorageDirectory(), "temp.jpg");
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }
        if (request == REQUEST_TAKE_PHOTO) {
            File f = getTmpFileForCamera();
            if (f.exists()) {
                rxChat.sendImage(f.getAbsolutePath());
                getView()
                        .hideAttachPannel();
            }
        } else if (request == REQUEST_CHOOS_FROM_GALLERY) {
            String picturePath = Utils.getGalleryPickedFilePath(getView().getContext(), data);
            if (picturePath != null){
                rxChat.sendImage(picturePath);
                getView()
                        .hideAttachPannel();
            }
        }
    }

    public RxChat getRxChat() {
        return rxChat;
    }
}
