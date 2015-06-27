package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.picasso.RxGlide;

import javax.inject.Inject;

public class StickerView extends ImageView {
    private final int MAX_SIZE;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject Stickers stickersInfo;

    private int height;
    private int width;

    public StickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
            MAX_SIZE = Math.min(512, calc.dp(126 + 32));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void bind(final TdApi.Sticker s) {
        setImageBitmap(null);
//        height = MAX_HEIGHT;
        float ratio;
        if (s.width == 0 || s.height == 0){
            if (s.sticker instanceof TdApi.FileLocal){
                String path = ((TdApi.FileLocal) s.sticker).path;
                TdApi.Sticker mapped = stickersInfo.getMappedSticker(path);
                if (mapped != null){
                    if (mapped.width == 0 || mapped.height == 0) {
                        ratio = 1f;
                    } else {
                        ratio = (float) mapped.width / mapped.height;
                    }
                } else {
                    ratio = 1f;
                }
            } else {
                ratio = 1f;
            }
        } else {
            ratio = (float) s.width / s.height;
        }
        if (ratio > 1) {
            width = MAX_SIZE;
            height = (int) (width / ratio);
        } else {
            height = MAX_SIZE;
            width = (int) (height * ratio);
        }


//        width = (int) (ratio * height);
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
