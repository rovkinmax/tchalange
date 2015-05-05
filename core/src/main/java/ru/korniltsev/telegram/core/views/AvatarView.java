package ru.korniltsev.telegram.core.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxGlide;
import ru.korniltsev.telegram.utils.R;

public class AvatarView extends ImageView {

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
        setImageBitmap(null);
        if (o instanceof TdApi.User) {
            picasso2.loadAvatarForUser((TdApi.User) o, size)
                    .transform(new RoundTransformation(getContext()))
                    .into(this);
        } else {
            picasso2.loadAvatarForChat((TdApi.Chat) o, size)
                    .transform(new RoundTransformation(getContext()))
                    .into(this);
        }
        ;
    }



    private class RoundTransformation extends BitmapTransformation {

        public RoundTransformation(Context context) {
            super(context);
        }

        @Override
        protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
            int width = toTransform.getWidth();
            int height = toTransform.getHeight();
            Bitmap transformed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(transformed);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);//todo thread locals
            p.setShader(new BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            canvas.drawCircle(width / 2, height / 2, width / 2, p);
            //            toTransform.recycle();
            return transformed;
        }

        @Override
        public String getId() {
            return "round";
        }
    }
}
