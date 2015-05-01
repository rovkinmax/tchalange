package ru.korniltsev.telegram.core.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxPicasso;
import ru.korniltsev.telegram.utils.R;

public class AvatarView extends View implements Target {


    private int size;
    private Paint paint;

    public final RxPicasso picasso2;
    private RectF rect;

    @Nullable private Bitmap bitmap;

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        picasso2 = ObjectGraphService.getObjectGraph(context)
                .get(RxPicasso.class);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarView);
        size = a.getDimensionPixelSize(R.styleable.AvatarView_size, -1);
        a.recycle();
        Assert.assertTrue(size != -1 && size > 0);

        rect = new RectF(0, 0, 0, 0);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(spec, spec);
    }

    /**
     * @param o can be TdApi.User or TdApi.Chat
     */
    public void loadAvatarFor(TdApi.TLObject o) {
        this.bitmap = null;
        picasso2.loadAvatar(o, size)
                .into(this);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        this.bitmap = bitmap;
        invalidate();
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        rect.set(0, 0, getWidth(), getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }
        //        else if (stub != null) {
        //            canvas.drawOval(rect, stubPaint);//todo generate i
        //            int height = staticLayout.getHeight();
        //            int p = (size - height) / 2;
        //            canvas.save();
        //            canvas.translate(0, p);
        //            staticLayout.draw(canvas);
        //            canvas.restore();

        //            float p = (size - stubWidth) / 2;
        //            int p2 = (size - stubTextsize )/ 2;
        //            canvas.drawText(stub, p, p2, stubTextPaint);
        //        }
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}
