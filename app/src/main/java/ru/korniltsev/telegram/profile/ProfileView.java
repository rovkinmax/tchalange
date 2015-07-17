package ru.korniltsev.telegram.profile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mortar.dagger1support.ObjectGraphService;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import static ru.korniltsev.telegram.common.AppUtils.call;
import static ru.korniltsev.telegram.common.AppUtils.copy;
import static ru.korniltsev.telegram.common.AppUtils.phoneNumberWithPlus;

public class ProfileView extends FrameLayout implements HandlesBack {
    @Inject
    ProfilePresenter presenter;

    @Inject
    PhoneFormat phoneFormat;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ProfileAdapter adapter;
    private ToolbarUtils toolbar;

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new ProfileAdapter(getContext(), presenter);
        adapter.addFirst(new ProfileAdapter.Item(0, "", "", null));//header
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        list.setAdapter(adapter);

        toolbar = ToolbarUtils.initToolbar(this)
                .pop();
        fakeToolbar = (FakeToolbar) findViewById(R.id.fake_toolbar);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);


        list.addOnScrollListener(
                fakeToolbar.createScrollListener(listLayout, list));
        fakeToolbar.initPosition(
                toolbar.toolbar);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }


    public void bindUser(@NonNull TdApi.User user) {
        fakeToolbar.bindUser(user);
        List<ProfileAdapter.Item> items = new ArrayList<>();

        if (!TextUtils.isEmpty(user.username)) {
            items.add(new ProfileAdapter.Item(
                    R.drawable.ic_user,
                    "@" + user.username,
                    getContext().getString(R.string.item_type_username),
                    null));
        }

        if (!TextUtils.isEmpty(user.phoneNumber)) {
            final String phone = phoneFormat.format(
                    phoneNumberWithPlus(user));
            items.add(new ProfileAdapter.Item(
                    R.drawable.phone_grey,
                    phone,
                    getContext().getString(R.string.item_type_mobile),
                    createPhoneActions(phone)));
        }
        items.add(ProfileAdapter.Item.getDivider());
        items.add(new ProfileAdapter.HorizontalItem(
                R.drawable.ic_setlock,
                getContext().getString(R.string.item_type_passcode),
                getPassCodeStatusString(user),
                null
        ));
        adapter.addAll(items);

    }

    private String getPassCodeStatusString(@NonNull TdApi.User user) {
        return getContext().getString(R.string.item_passcode_disabled);
    }

    private List<ListChoicePopup.Item> createPhoneActions(final String phone) {

        final ArrayList<ListChoicePopup.Item> data = new ArrayList<>();
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.call_phone), new Runnable() {
            @Override
            public void run() {
                call(getContext(), phone);
            }
        }));
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.copy_phone), new Runnable() {
            @Override
            public void run() {
                copy(getContext(), phone);
            }
        }));
        return data;
    }

    @Override
    public boolean onBackPressed() {
        return presenter.hidePopup();
    }


}
