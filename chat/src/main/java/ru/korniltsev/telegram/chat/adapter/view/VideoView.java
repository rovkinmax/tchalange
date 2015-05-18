package ru.korniltsev.telegram.chat.adapter.view;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import java.io.File;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class VideoView extends FrameLayout {

    private final int dp207;
    private final int dp154;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject RxDownloadManager downloader;

//    private ImageView actionIcon;
    private ImageView preview;

    private TdApi.Video msg;
    private DownloadView downloadView;
    private int width;
    private int height;

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
        this.msg = msg;
        float ratio = (float)msg.thumb.width / msg.thumb.height;//PhotoUtils.getPhotoRation(msg.thumb.);
        if (ratio > 1) {
            width = dp207;
        } else {
            width = dp154;
        }
        height = (int) ( width/ratio);
        requestLayout();
        showLowQualityThumb();
        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, false, false, 48);
        downloadView.bind(msg.video, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {

            }

            @Override
            public void onFinished(TdApi.FileLocal e) {

            }

            @Override
            public void play(TdApi.FileLocal e) {
                playVideo(e);
            }
        }, this);


    }

    private void showLowQualityThumb() {
        picasso.loadPhoto(msg.thumb.photo, false)
                .resize(width, height)
                .into(preview);
    }
}
