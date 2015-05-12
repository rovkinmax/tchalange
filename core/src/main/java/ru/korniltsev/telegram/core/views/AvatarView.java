package ru.korniltsev.telegram.core.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import com.squareup.picasso.Transformation;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.utils.R;

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
    public void loadAvatarFor(TdApi.TLObject o) {
        setImageBitmap(null);
        if (o instanceof TdApi.User) {
            picasso2.loadAvatarForUser((TdApi.User) o, size)
                    .transform(new RoundTransformation())
                    .into(this);
        } else {
            picasso2.loadAvatarForChat((TdApi.Chat) o, size)
                    .transform(new RoundTransformation())
                    .into(this);
        }
    }

    @Override
    public void requestLayout() {
        if (getMeasuredWidth() == 0){
            super.requestLayout();
        }
    }

    private static class RoundTransformation implements Transformation {

        private static final ThreadLocal<Paint> paints = new ThreadLocal<Paint>(){
            @Override
            protected Paint initialValue() {
                return new Paint(Paint.ANTI_ALIAS_FLAG);
            }
        };


        @Override
        public Bitmap transform(Bitmap source) {
            int width = source.getWidth();
            int height = source.getHeight();
            Bitmap transformed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(transformed);
            Paint p = paints.get();
            p.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            canvas.drawCircle(width / 2, height / 2, width / 2, p);
            source.recycle();
            return transformed;
        }

        @Override
        public String key() {
            return "round transformation";
        }
    }
}
