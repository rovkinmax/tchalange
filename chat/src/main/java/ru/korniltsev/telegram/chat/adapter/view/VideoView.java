package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.telegram.android.DpCalculator;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.RxGlide;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import java.io.File;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class VideoView extends FrameLayout {

    private final int widthSpec;
    private final int heightSpec;
    private final int width;
    private final int height;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject RxDownloadManager downloader;

    private ImageView actionIcon;
    private ImageView preview;

    private TdApi.Video msg;
    private Subscription subscription = Subscriptions.empty();

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        //207x165
        width = calc.dp(207);
        height = calc.dp(165);
        widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        preview = ((ImageView) findViewById(R.id.preview));
        actionIcon = ((ImageView) findViewById(R.id.btn_download));
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloader.isDownloaded(msg.video)) {
                    play();
                } else {
                    download();
                }
            }
        });
    }

    private void play() {
        TdApi.FileLocal f = (TdApi.FileLocal) msg.video;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(f.path)), "video/mp4");
        getContext().startActivity(intent);
    }

    private void download() {
        downloader.download((TdApi.FileEmpty) msg.video);
        set(msg);//update ui
    }

    private void subscribeForFileDownload(final TdApi.FileEmpty file) {
        subscription = downloader.observableFor(file)
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.UpdateFile>() {
                    @Override
                    public void call(TdApi.UpdateFile update) {
                        msg.video = new TdApi.FileLocal(update.fileId, update.size, update.path);
                        set(msg);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthSpec, heightSpec);
    }

    public void set(TdApi.Video msg) {
        //        if (msg == this.msg) {
        //            return;
        //        }
        this.msg = msg;

        subscription.unsubscribe();

        if (downloader.isDownloaded(msg.video)) {
            //show play
            actionIcon.setImageResource(R.drawable.ic_play);
            setEnabled(true);
            //todo show high quality preview
        } else if (downloader.isDownloading((TdApi.FileEmpty) msg.video)) {
            //show pause
            actionIcon.setImageResource(R.drawable.ic_pause);
            //that does nothing
            setEnabled(false);

            //subscribe for update
            subscribeForFileDownload((TdApi.FileEmpty) msg.video);
            showLowQualityThumb();
        } else {
            //show download Button
            actionIcon.setImageResource(R.drawable.ic_download);
            setEnabled(true);
            showLowQualityThumb();
        }
    }

    private void showLowQualityThumb() {
        //        Context applicationContext = getContext().getApplicationContext();
        picasso.loadPhoto(msg.thumb.photo)
                .override(width, height)
                        //                        .transform(
                        //                                new CropAndBlurTransformation(applicationContext, width, height))
                .into(preview);
    }
}
