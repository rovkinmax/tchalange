package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import ru.korniltsev.telegram.core.views.AvatarStubColors;
import ru.korniltsev.telegram.utils.R;

import java.io.IOException;

public class StubRequestHandler extends RequestHandler {
    public static final String SIZE = "size";
    public static final String ID = "id";
    public static final String CHARS = "chars";
    private  final ThreadLocal<RectF> rects = new ThreadLocal<RectF>() {
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

    public static final String TELEGRAM_STUB = "telegram.stub";
    private final int stubTextSize;

    public static Uri create(String chars, int id, int size) {
        return new Uri.Builder()
                .scheme(TELEGRAM_STUB)
                .appendQueryParameter(CHARS, chars)
                .appendQueryParameter(ID, String.valueOf(id))
                .appendQueryParameter(SIZE, String.valueOf(size))
                .build();
    }

    final Context ctx;

    public StubRequestHandler(Context ctx) {
        this.ctx = ctx;
        stubTextSize = this.ctx.getResources().getDimensionPixelSize(R.dimen.avatar_text_size);//todo wrong size!!

    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.uri.getScheme().equals(TELEGRAM_STUB);
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        Uri uri = request.uri;
        int size = Integer.parseInt(uri.getQueryParameter(SIZE));
        int id = Integer.parseInt(uri.getQueryParameter(ID));
        String chars = uri.getQueryParameter(CHARS);
        int colorFor = AvatarStubColors.getColorFor(id);

        TextPaint stubTextPaint = textPaints.get();
        stubTextPaint.setTextSize(stubTextSize);
        stubTextPaint.setColor(Color.WHITE);//todo!!!!
        Paint paint = paints.get();
        paint.setColor(colorFor);

        StaticLayout staticLayout = new StaticLayout(chars, stubTextPaint, size, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
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
        return new Result(bmp, Picasso.LoadedFrom.NETWORK);
    }
}
