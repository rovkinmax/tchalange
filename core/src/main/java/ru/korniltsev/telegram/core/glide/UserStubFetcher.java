package ru.korniltsev.telegram.core.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import ru.korniltsev.telegram.core.views.AvatarStubColors;
import ru.korniltsev.telegram.utils.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UserStubFetcher implements DataFetcher<InputStream> {

    private static final ThreadLocal<RectF> rects = new ThreadLocal<RectF>() {
        @Override
        protected RectF initialValue() {
            return new RectF();
        }
    };

    private final ThreadLocal<Paint> paints = new ThreadLocal<Paint>() {
        @Override
        protected Paint initialValue() {
            return new Paint(Paint.ANTI_ALIAS_FLAG);
        }
    };

    private final ThreadLocal<TextPaint> textPaints = new ThreadLocal<TextPaint>() {
        @Override
        protected TextPaint initialValue() {
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

            return textPaint;
        }
    };

    final int width;
    final int height;
    final Stub stub;
    private final String id;
    final Context ctx;
    private final int stubTextSize;

    public UserStubFetcher(Stub stub, int height, int width, Context ctx) {
        this.stub = stub;
        this.ctx = ctx;
        this.id = "user.stub." + stub.id + "." + stub.chars;
        this.height = height;
        this.width = width;
        stubTextSize = this.ctx.getResources().getDimensionPixelSize(R.dimen.avatar_text_size);//todo wrong size!!
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        int size = width;
        int colorFor = AvatarStubColors.getColorFor(stub.id);

        TextPaint stubTextPaint = textPaints.get();
        stubTextPaint.setTextSize(stubTextSize);
        stubTextPaint.setColor(Color.WHITE);//todo!!!!
        Paint paint = paints.get();
        paint.setColor(colorFor);

        StaticLayout staticLayout = new StaticLayout(stub.chars, stubTextPaint, size, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);//todo mb less?
        Canvas canvas = new Canvas(bmp);
        RectF r = rects.get();
        r.set(0, 0, size, size);
        canvas.drawOval(r, paint);
        int height = staticLayout.getHeight();
        int p = (size - height) / 2;
        canvas.save();
        canvas.translate(0, p);
        staticLayout.draw(canvas);
        canvas.restore();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public void cleanup() {

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void cancel() {

    }
}
