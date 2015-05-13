package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.Utils.uiName;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatView extends LinearLayout {
    public static final int SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT = 10;
    @Inject Presenter presenter;
    @Inject RxGlide picasso;

    private RecyclerView list;
    private MessagePanel messagePanel;
    private LinearLayoutManager layout;
    private ToolbarUtils toolbar;
    private AvatarView toolbarAvatar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;

    private Adapter adapter;
    private final int toolbarAvatarSize;

    private Runnable viewSpanNotFilledAction = new Runnable() {
        @Override
        public void run() {
            presenter.listScrolledToEnd();
        }
    };
    private View btnScrollDown;
    private View emptyView;

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
        btnScrollDown = findViewById(R.id.scroll_down);
        messagePanel.setListener(presenter);

        layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        adapter = new Adapter(getContext(), picasso);
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        btnScrollDown.setVisibility(View.INVISIBLE);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, /*waitForLastItem*/  new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }) {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        updateBtnScrollDown();
                    }
                });

        btnScrollDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.scrollToPosition(0);
                list.stopScroll();
                btnScrollDown.setVisibility(View.INVISIBLE);
            }
        });
        emptyView = findViewById(R.id.empty_view);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (adapter.getItemCount() == 0){
                    if (emptyView.getVisibility() == INVISIBLE){
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setAlpha(0f);
                        emptyView.animate()
                                .alpha(1);
                    }
                } else {
                    emptyView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void updateBtnScrollDown() {
        if (layout.findFirstVisibleItemPosition() >= SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT){
            btnScrollDown.setVisibility(View.VISIBLE);
        } else {
            btnScrollDown.setVisibility(View.INVISIBLE);
        }
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
                uiName(user));
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

    public void updateData(RxChat rxChat) {
        Adapter a = getAdapter();
        a.setChat(rxChat);
        a.setData(rxChat.getMessages());

        CheckRecyclerViewSpan.check(list, viewSpanNotFilledAction);
    }

    public void addNewMessage(List<RxChat.ChatListItem> message) {
        boolean scrollDown = layout.findFirstCompletelyVisibleItemPosition() == 0;
        adapter.addFirst(message);
        if (scrollDown){
            layout.scrollToPosition(0);
        }
    }


}
