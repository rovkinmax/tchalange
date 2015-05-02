package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.view.MessagePanel;
import ru.korniltsev.telegram.core.adapters.TargetAdapter;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;

import javax.inject.Inject;
import java.util.Date;

import static junit.framework.Assert.assertNotNull;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatView extends LinearLayout {
    @Inject Presenter presenter;
    //    @Inject RxPicasso picasso;

    private RecyclerView list;
    private MessagePanel messagePanel;
    private LinearLayoutManager layout;
    private ToolbarUtils toolbar;
    private AvatarView toolbarAvatar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;

    private Adapter adapter;
    private final int toolbarAvatarSize;

    private Target target;

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        toolbarAvatarSize = context.getResources().getDimensionPixelSize(R.dimen.toolbar_avatar_size);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = initToolbar(this)
                .pop()
                .customView(R.layout.toolbar_chat_title)
                .inflate(R.menu.chat_fragment)
                .setMenuClickListener(presenter);
        View customView = toolbar.getCustomView();
        assertNotNull(customView);
        toolbarAvatar = ((AvatarView) customView.findViewById(R.id.chat_avatar));
        toolbarTitle = ((TextView) customView.findViewById(R.id.title));
        toolbarSubtitle = ((TextView) customView.findViewById(R.id.subtitle));
        list = (RecyclerView) findViewById(R.id.list);
        messagePanel = (MessagePanel) findViewById(R.id.message_panel);
        messagePanel.setListener(presenter);

        layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
//        layout.setStackFromEnd(true);
        adapter = new Adapter(getContext());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, /*waitForLastItem*/ true, new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }));
        this.target = new TargetAdapter() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                toolbar.setIcon(bitmap);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }


    public Adapter getAdapter() {
        return adapter;
    }

    public void addHistory(Adapter.Portion messages) {
        adapter.addHistory(messages);
    }

    public void initMenu(boolean groupChat) {
        if (!groupChat) {
            toolbar.hideMenu(R.id.menu_leave_group);
        }
    }

    public void clearAdapter() {
        adapter.clearData();
    }

    public void loadToolBarImage(TdApi.Chat chat) {
        toolbarAvatar.loadAvatarFor(chat);
    }

    public void setGroupChatTitle(TdApi.GroupChat groupChat) {
        toolbarTitle.setText(
                groupChat.title);
    }

    public void setPrivateChatTitle(TdApi.User user) {
        toolbarTitle.setText(
                user.firstName + " " + user.lastName);
    }

    public void setwGroupChatSubtitle(int total, int online) {
        //todo updates
        Resources res = getResources();
        String totalStr = res.getQuantityString(R.plurals.group_chat_members, total, total);
        String onlineStr = res.getQuantityString(R.plurals.group_chat_members_online, online, online);
        toolbarSubtitle.setText(
                totalStr + ", " + onlineStr);
    }

    public void setPirvateChatSubtitle(TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) {
            toolbarSubtitle.setText(R.string.user_status_online);
        } else if (status instanceof TdApi.UserStatusOffline) {
            long wasOnline = ((TdApi.UserStatusOffline) status).wasOnline;
            Date date = new Date(wasOnline * 1000);
            toolbarSubtitle.setText(date.toString());//todo time zone
        } else if (status instanceof TdApi.UserStatusLastWeek) {
            toolbarSubtitle.setText(R.string.user_status_last_week);
        } else if (status instanceof TdApi.UserStatusLastMonth) {
            toolbarSubtitle.setText(R.string.user_status_last_month);
        } else if (status instanceof TdApi.UserStatusRecently) {
            toolbarSubtitle.setText(R.string.user_status_recently);
        } else {
            //empty
        }
    }

    public void addNewMessage(Adapter.Portion portion) {
        int lastVisible = layout.findFirstCompletelyVisibleItemPosition();
        boolean scrollToBottom = false;
        if (lastVisible == 0) {
            scrollToBottom = true;
        }
        adapter.insertNewMessage(portion);
        if (scrollToBottom) {
            layout.scrollToPosition(0);
        }
    }






}
