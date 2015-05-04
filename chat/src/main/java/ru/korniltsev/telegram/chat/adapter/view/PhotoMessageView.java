package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxPicasso;

import javax.inject.Inject;

//todo draw only bitmap
public class PhotoMessageView extends ImageView {
    public static final int ZERO_MEASURE_SPEC = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
    private final int atmost;
    @Inject RxPicasso picasso;
    private TdApi.MessagePhoto photo;

    public PhotoMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        Resources res = context.getResources();
        int avatarSize = res.getDimensionPixelSize(R.dimen.avatar_size);
        int avatarMargins = res.getDimensionPixelSize(R.dimen.avatar_margin_left) + res.getDimensionPixelSize(R.dimen.avatar_margin_right);
        int myMargin = res.getDimensionPixelSize(R.dimen.photo_view_right_margin);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int width = display.getWidth();
        atmost = width - myMargin - avatarSize - avatarMargins;
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (selectedSize == null) {
            super.onMeasure(ZERO_MEASURE_SPEC, ZERO_MEASURE_SPEC);
        } else {
            int w = MeasureSpec.makeMeasureSpec(selectedSize.width, MeasureSpec.EXACTLY);
            int h = MeasureSpec.makeMeasureSpec(selectedSize.height, MeasureSpec.EXACTLY);
            super.onMeasure(w, h);
        }

    }

    //    Thumbnail type and its sizes
    //
    //    Type	Image filter	Size limits, px
    //    s	box	100x100
    //    m	box	320x320
    //    x	box	800x800
    //    y	box	1280x1280
    //    w	box	2560x2560
    //    a	crop	160x160
    //    b	crop	320x320
    //    c	crop	640x640
    //    d	crop	1280x1280

    //todo sort
    //null if there we try to display image with no sizes less then our max width
    @Nullable private TdApi.PhotoSize selectedSize;
    public void load(TdApi.MessagePhoto photo) {
        if (this.photo == photo){
            return;
        }
        this.photo = photo;
        setImageDrawable(null);
        TdApi.PhotoSize[] photos = photo.photo.photos;
        selectedSize = null;
        if (photos.length != 0) {
            for (int i = photos.length - 1; i >= 0; i--) {
                TdApi.PhotoSize it = photos[i];
                if (it.width <= atmost) {
                    selectedSize = it;
                    break;
                }
            }
        }
        if (selectedSize != null) {

            //there is probably a chance that it w
            picasso.loadPhoto(selectedSize.photo)
                    .into(this);
        }
    }
}
