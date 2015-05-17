package ru.korniltsev.telegram.core.views;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import com.squareup.picasso.Transformation;

public class RoundTransformation implements Transformation {

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
