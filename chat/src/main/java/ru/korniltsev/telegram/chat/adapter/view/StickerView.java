package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.telegram.android.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;

import javax.inject.Inject;

public class StickerView extends ImageView {
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;

    final int height;
    private int width;

    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        height = calc.dp(256);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void bind(final TdApi.Sticker s) {
        setImageBitmap(null);

        float ratio = (float) s.width / s.height;

        width = (int) (ratio * height);
        picasso.loadPhoto(s.thumb.photo, true)
                .resize(width, height)
                .priority(Picasso.Priority.HIGH)
                .into(this, new Callback() {
                    @Override
                    public void onSuccess() {
                        picasso.loadPhoto(s.sticker, true)
                                .placeholder(getDrawable())
                                .resize(width, height)
                                .into(StickerView.this);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }
}
