package ru.korniltsev.telegram.chat.adapter.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import pl.droidsonroids.gif.GifDrawable;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.views.DownloadView;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

public class VideoView extends FrameLayout {

    private final int dp207;
    private final int dp154;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject RxDownloadManager downloader;

//    private ImageView actionIcon;
    private ImageView preview;

//    private TdApi.Video msg;
    private DownloadView downloadView;
    private int width;
    private int height;
    private TdApi.PhotoSize thumb;

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        //207x165
        dp207 = calc.dp(207);
        dp154 = calc.dp(154);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        preview = ((ImageView) findViewById(R.id.preview));
        downloadView = ((DownloadView) findViewById(R.id.download_view));
    }

    private void playVideo(TdApi.FileLocal f) {
        File src = new File(f.path);


        File exposed = downloader.exposeFile(src, Environment.DIRECTORY_DOWNLOADS, null);

        Uri uri = Uri.fromFile(exposed);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/*");
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //todo error
        }
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }



    public void set(TdApi.Video msg) {
//        this.msg = msg;
        TdApi.PhotoSize thumb = msg.thumb;
        TdApi.File file = msg.video;
        bindGeneral(thumb, file, false);
    }

    public void set(TdApi.Document doc) {
        assertTrue("image/gif".equals(doc.mimeType));
        TdApi.PhotoSize thumb = doc.thumb;
        TdApi.File file = doc.document;
        bindGeneral(thumb, file, true);
    }

    private void bindGeneral(TdApi.PhotoSize thumb, TdApi.File file, final boolean gif) {
        this.thumb = thumb;
        float ratio = (float) thumb.width / thumb.height;
        if (ratio > 1) {
            width = dp207;
        } else {
            width = dp154;
        }
        height = (int) (width / ratio);
        showLowQualityThumb(thumb);
        requestLayout();

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, false, false, 48);
        downloadView.setVisibility(View.VISIBLE);
        downloadView.bind(file, cfg, new DownloadView.CallBack() {
            @Override
            public void onFinished(TdApi.FileLocal e, boolean justDownloaded) {
                if (gif && justDownloaded) {
                    setAndPlayGif(e);
                }
            }

            @Override
            public void play(TdApi.FileLocal e) {
                if (gif) {
                    Drawable drawable = preview.getDrawable();
                    if (drawable instanceof GifDrawable){
                        ((GifDrawable) drawable).pause();
                        preview.setImageDrawable(null);
                        showLowQualityThumb(VideoView.this.thumb);
                        downloadView.setVisibility(View.VISIBLE);
                    } else {
                        setAndPlayGif(e);
                    }
                } else {
                    playVideo(e);
                }
            }
        }, this);
    }

    private void setAndPlayGif(TdApi.FileLocal e) {
        try {
            preview.setImageDrawable(new GifDrawable(e.path));
            downloadView.setVisibility(View.GONE);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void playGif(TdApi.FileLocal e) {
        Drawable drawable = preview.getDrawable();
        if (drawable instanceof GifDrawable){
            preview.setImageDrawable(null);
            showLowQualityThumb(thumb);
        } else {

        }
//        try {
//            preview.setImageDrawable(new GifDrawable(e.path));
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
    }

    private void showLowQualityThumb(TdApi.PhotoSize thumb) {
        picasso.loadPhoto(thumb.photo, false)
                .resize(width, height)
                .into(preview);
    }
}
