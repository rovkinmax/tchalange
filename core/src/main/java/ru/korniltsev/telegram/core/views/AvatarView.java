package ru.korniltsev.telegram.core.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.crashlytics.android.core.CrashlyticsCore;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.utils.R;

import static junit.framework.Assert.assertNotNull;

public class AvatarView extends ImageView {


    private final int spec;
    private int size;

    public final RxGlide picasso2;

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        picasso2 = ObjectGraphService.getObjectGraph(context)
                .get(RxGlide.class);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarView);
        size = a.getDimensionPixelSize(R.styleable.AvatarView_size, -1);
        a.recycle();
        Assert.assertTrue(size != -1 && size > 0);

        spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(size, size);
    }



    /**
     * @param o can be TdApi.User or TdApi.Chat
     */
    public void loadAvatarFor(@NonNull TdApi.TLObject o) {
        if (o == null) {
            CrashlyticsCore.getInstance()
                    .logException(new NullPointerException());
            setImageBitmap(null);
            picasso2.getPicasso().cancelRequest(this);
            return;
        }
//        assertNotNull(o);
        setImageBitmap(null);
        if (o instanceof TdApi.User) {
            picasso2.loadAvatarForUser((TdApi.User) o, size, this);
            //                    .transform(ROUND)
            //                    .into(this);
        } else {
            picasso2.loadAvatarForChat((TdApi.Chat) o, size, this);
            //                    .transform(new RoundTransformation())
            //                    .into(this);
        }
    }

    @Override
    public void requestLayout() {
        if (getMeasuredWidth() == 0){
            super.requestLayout();
        }
    }

    public int getSize() {
        return size;
    }
}
