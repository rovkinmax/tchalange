package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;

import javax.inject.Inject;
import java.util.List;

import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;
import static rx.android.view.ViewObservable.clicks;

public class ChatListView extends DrawerLayout {

    @Inject ChatList.Presenter presenter;
    @Inject RXClient client;

    private RecyclerView list;
    private AvatarView drawerAvatar;
    private TextView drawerName;
    private TextView drawerPhone;
    private View btnLogout;

    private Adapter adapter;
    private ToolbarUtils toolbar;
    private LinearLayoutManager layout;

    public ChatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        injectViews();
        //list
        adapter = new Adapter(getContext(), presenter::openChat);
        layout = new LinearLayoutManager(getContext());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, presenter::listScrolledToEnd));

        //toolbar
        toolbar = initToolbar(this)
                .setDrawer(this, R.string.navigation_drawer_open, R.string.navigation_drawer_close);//todo what is open and clos?

        //logout
        clicks(btnLogout)
                .subscribe(presenter::logout);


    }

    private void injectViews() {
        list = (RecyclerView) this.findViewById(R.id.list);
        drawerAvatar = (AvatarView) this.findViewById(R.id.drawer_avatar);
        drawerName = ((TextView) this.findViewById(R.id.drawer_name));
        drawerPhone = ((TextView) this.findViewById(R.id.drawer_phone));
        btnLogout = this.findViewById(R.id.btn_logout);
    }

    public void addChats(List<TdApi.Chat> chats) {
        adapter.addAll(chats);


    }

    public void showMe(TdApi.User user) {
        drawerAvatar.loadAvatarFor(user);
        drawerName.setText(
                user.lastName + " " + user.firstName);
        drawerPhone.setText(
                user.phoneNumber);
    }

    public void updateNetworkStatus(boolean connected) {
        toolbar.setTitle(connected ? R.string.messages : R.string.waiting_for_connection);
    }
}
