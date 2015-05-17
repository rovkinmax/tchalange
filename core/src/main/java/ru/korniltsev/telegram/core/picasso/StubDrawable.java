package ru.korniltsev.telegram.core.picasso;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import ru.korniltsev.telegram.core.views.AvatarStubColors;

public class StubDrawable extends Drawable {

    private final RectF rectF;
    private final StaticLayout staticLayout;
    private final Paint colorPaint;
    private final RxGlide.StubKey key;
    private final TextPaint textPaint;

    public StubDrawable(RxGlide.StubKey key) {
        this.key = key;
        this.rectF = new RectF(0, 0, key.size, key.size);
        colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(key.size / 2.5f);
        textPaint.setColor(Color.WHITE);
        colorPaint.setColor(AvatarStubColors.getColorFor(key.id));
        staticLayout = new StaticLayout(key.chars, textPaint, key.size, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
    }
    //    private final RectF rectF;

    //    public StubDrawable(StaticLayout layout, int color, int size) {
    //        this.layout = layout;
    //        this.color = color;
    //        this.size = size;
    //        this.rectF = new RectF(0, 0, size, size);
    //
    //                TextPaint stubTextPaint = textPaints.get();
    //                stubTextPaint.setTextSize(stubTextSize);
    //                stubTextPaint.setColor(Color.WHITE);//todo!!!!
    //                Paint paint = paints.get();
    //                paint.setColor(colorFor);
    //
    //                StaticLayout staticLayout = new StaticLayout(chars, stubTextPaint, size, Layout.Alignment.ALIGN_CENTER, 1, 0, false);

    //    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawOval(rectF, colorPaint);
        int height = staticLayout.getHeight();
        int p = (key.size - height) / 2;
        canvas.save();
        canvas.translate(0, p);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
