package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.utils.Colors;

import javax.inject.Inject;

public class ForwardedMessageView extends RelativeLayout {

    public static final int BLUE_LINE_WIDTH_DP = 3;
    private final int lineWidth;
    private final int leftAnchor;

    private View forwardAvatar;
    private View nick;
    private View forwardText;
    @Inject DpCalculator calc;
    @Nullable private View avatar;

    public ForwardedMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);

        setWillNotDraw(false);
        lineWidth = calc.dp(BLUE_LINE_WIDTH_DP);
        leftAnchor = calc.dp(61);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        forwardAvatar = findViewById(R.id.forward_avatar);
        avatar = findViewById(R.id.avatar);
        forwardText = findViewById(R.id.forward_text);

        nick = findViewById(R.id.nick);
    }

    final Paint p = new Paint();

    {
        p.setColor(Colors.USER_NAME_COLOR);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int top = avatar == null ? 0 : forwardAvatar.getTop();
        int left = leftAnchor;
        int bottom = Math.max(forwardText.getBottom(), forwardAvatar.getBottom());

        canvas.drawRect(left, top, left + lineWidth, bottom, p);
    }
}
