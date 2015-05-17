package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;

import javax.inject.Inject;

public class StickerView extends ImageView {
    private final int MAX_HEIGHT;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;

    private int height;
    private int width;

    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        MAX_HEIGHT = calc.dp(256);
//        height = MAX_HEIGHT;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void bind(final TdApi.Sticker s) {
        setImageBitmap(null);
        height = Math.min(MAX_HEIGHT, s.height);
        float ratio = (float) s.width / s.height;

        width = (int) (ratio * height);
        if (isValidThumb(s)){
            picasso.loadPhoto(s.thumb.photo, true)
//                    .resize(width, height)
                    .priority(Picasso.Priority.HIGH)
                    .into(this, new Callback() {
                        @Override
                        public void onSuccess() {
                            picasso.loadPhoto(s.sticker, true)
                                    .placeholder(getDrawable())
//                                    .resize(width, height)
                                    .into(StickerView.this);
                        }

                        @Override
                        public void onError() {

                        }
                    });
        } else {
            picasso.loadPhoto(s.sticker, true)
//                    .resize(width, height)
                    .into(StickerView.this);

        }

    }

    private boolean isValidThumb(TdApi.Sticker s) {
        TdApi.File photo = s.thumb.photo;
        int id;
        if (photo instanceof TdApi.FileLocal) {
            id = ((TdApi.FileLocal) photo).id;
            return id != 0;
        } else {
            id = ((TdApi.FileEmpty) photo).id;
        }
        return id != 0;
    }
}
