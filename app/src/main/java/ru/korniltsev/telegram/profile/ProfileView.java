package ru.korniltsev.telegram.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.common.AppUtils.uiName;
import static ru.korniltsev.telegram.common.AppUtils.uiUserStatus;

public class ProfileView extends FrameLayout implements HandlesBack{
    @Inject ProfilePresenter presenter;
    @Inject DpCalculator calc;
    @Inject ChatDB chats;
    @Inject PhoneFormat phoneFormat;

    private RecyclerView list;
    private AvatarView image;
    private LinearLayoutManager listLayout;
    private ToolbarUtils toolbar;
    private TextView title;
    private TextView subTitle;
    private ViewGroup titleParent;
    private int headerHeight;
    private int toolbarHeight;
    private ProfileAdapter adapter;

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        list = ((RecyclerView) findViewById(R.id.list));

        listLayout = new LinearLayoutManager(getContext());
        list.setLayoutManager(listLayout);

        adapter = new ProfileAdapter(getContext(), presenter);
        adapter.addFirst(new ProfileAdapter.Item(0,"", "", null));//header

        list.setAdapter(adapter);
        image = ((AvatarView) findViewById(R.id.avatar));
        toolbar = ToolbarUtils.initToolbar(this)
                .pop();
        title = ((TextView) findViewById(R.id.title));
        subTitle = ((TextView) findViewById(R.id.subtitle));
        titleParent = ((ViewGroup) title.getParent());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);

        headerHeight = getContext().getResources().getDimensionPixelSize(R.dimen.profile_header_height);
        toolbarHeight = toolbar.toolbar.getLayoutParams().height;
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                final int pos = listLayout.findFirstVisibleItemPosition();
                float res ;
                if (pos == 0) {
                    final View childAt = list.getChildAt(0);
                    final int bottom = childAt.getBottom();
                    if (bottom <= toolbarHeight){
                        res = 1f;
                    } else {
                        res = 1f - (float) (bottom- toolbarHeight) / (headerHeight - toolbarHeight);
                    }
                } else {
                    res = 1f;
                }
                positionAvatar(res);

            }
        });
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
                final RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
                if (vh.getAdapterPosition() == 1){
                    outRect.top = calc.dp(16);
                }
            }
        });
        positionAvatar(0f);
    }

    private void positionAvatar(float res) {
        int dp4 = calc.dp(4);
        int initialAvatarSize = image.getSize();
        int targetSize = dp4 * 10;
        int fullDiff = targetSize - initialAvatarSize;
        float diffRes = res * fullDiff;
        float scaleResult = 1 + diffRes/initialAvatarSize;
        image.setScaleX( scaleResult);
        image.setScaleY(scaleResult);

        int dp16 = dp4 * 4;
        int avatarMargin = dp16;
        int ty = headerHeight - initialAvatarSize/2 - toolbarHeight /2 - avatarMargin;
        image.setTranslationY(-avatarMargin - ty * res);



        image.setTranslationX(dp16 + res * dp4 * 7);


        //text

        //width
        int maxWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int initialTextX = initialAvatarSize + 2 * avatarMargin;
        int initialWidth = maxWidth - initialTextX;
        int targetWidth = initialWidth - dp4 * 14;
        int diff = initialWidth - targetWidth;
        int finalWidth = (int) (initialWidth - res * diff);
        final ViewGroup.LayoutParams lp = title.getLayoutParams();
        if (lp.width != finalWidth){//todo round!
            lp.width = finalWidth;
            title.setLayoutParams(lp);
        }

        //text ypos
        final int titleParentHeight = titleParent.getLayoutParams().height;
        final int initialTranslationY = -avatarMargin + (initialAvatarSize - titleParentHeight) / 2;
        final int targetTranslationY = - (headerHeight - titleParentHeight);
        final int diff2 = targetTranslationY - initialTranslationY;
        final int absDiff2 = (int) (diff2 * res);
        titleParent.setTranslationY(initialTranslationY + absDiff2);
        //text xpos
        final int targetTextX = initialTextX + calc.dp(12);
        final int diff3 = targetTextX - initialTextX;

        titleParent.setTranslationX(initialTextX + res * diff3);


    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }



    public void bindUser(@NonNull TdApi.User user) {
        title.setText(
                uiName(user, getContext()));
        image.loadAvatarFor(user);
        subTitle.setText(
                uiUserStatus(getContext(), chats.getUserStatus(user)));

        List<ProfileAdapter.Item> items = new ArrayList<>();
        if (!TextUtils.isEmpty(user.phoneNumber)){
            final String phone = phoneFormat.format(
                    phoneNumberWithPlus(user));
            items.add(new ProfileAdapter.Item(
                    R.drawable.phone_grey,
                    phone,
                    "mobile",
                    createPhoneActions(phone)));
        }
        if (!TextUtils.isEmpty(user.username)){
            items.add(new ProfileAdapter.Item(
                    0,
                    "@" + user.username,
                    "userName",
                    null));
        }
        adapter.addAll(items);

    }

    private List<ListChoicePopup.Item> createPhoneActions(final String phone) {
        
        final ArrayList<ListChoicePopup.Item> data = new ArrayList<>();
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.call_phone), new Runnable(){
            @Override
            public void run() {
                call(phone);
            }
        }));
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.copy_phone), new Runnable(){
            @Override
            public void run() {
                copy(phone);
            }
        }));
        return data;
    }

    private void copy(String phone) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("phone", phone);
        clipboard.setPrimaryClip(clip);
    }

    private void call(String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        getContext()
                .startActivity(intent);
    }

    @NonNull
    private String phoneNumberWithPlus(@NonNull TdApi.User user) {
        final String phoneNumber = user.phoneNumber;
        if (phoneNumber.startsWith("+")){
            return phoneNumber;
        } else {
            return "+" + user.phoneNumber;
        }
    }

    public void bindChat(@NonNull TdApi.Chat chat) {
        final TdApi.GroupChatInfo groupChat = (TdApi.GroupChatInfo) chat.type;
        title.setText(groupChat.groupChat.title);
        image.loadAvatarFor(chat);
        final int participantsCount = groupChat.groupChat.participantsCount;
        final Resources resources = getContext().getResources();
        subTitle.setText(
                resources.getQuantityString(R.plurals.group_chat_members, participantsCount)
        );
    }

    @Override
    public boolean onBackPressed() {
        return presenter.hidePopup();
    }
}
