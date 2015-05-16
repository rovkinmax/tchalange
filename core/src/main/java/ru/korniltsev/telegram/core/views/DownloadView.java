package ru.korniltsev.telegram.core.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.utils.R;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class DownloadView extends FrameLayout {
    public static final int LEVE_DOWNLOAD = 0;
    public static final int LEVEL_PAUSE = 1;
    public static final int LEVEL_EMPTY = 2;
    public static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();
    public static final int BG_BLUE = 0xffecf4F9;
    public static final int BG_BLACK = 0xBE000000;
    public static final int BG_DARKEN_BLUE = 0xff579cda;

    //size of visible part of view
//    private final int size;

//    private final Drawable icDownload;
//    private final Drawable icPause;
    private final Paint bgPaint;
    private final ImageView icon;
    private final RectF rect;
    private final Paint p;
//    private final boolean finalIconEmpty;
    private final Drawable downloadBlue;
    private final Drawable download;
    private final Drawable pauseBlue;
    private final Drawable ause;
    private final int strokeWidth;

    float progress;

    @Inject RxDownloadManager downloader;
    @Inject DpCalculator calc;
    private TdApi.File file;
    private boolean drawProgress;
    private ObjectAnimator animator;
    private CallBack cb;
    private Config cfg;
    private int size;

    public DownloadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ObjectGraphService.inject(context, this);
//        size = s;


        Resources res = context.getResources();
        downloadBlue = res.getDrawable(R.drawable.ic_download_blue);
        download = res.getDrawable(R.drawable.ic_download);
        pauseBlue = res.getDrawable(R.drawable.ic_pause_blue);
        ause = res.getDrawable(R.drawable.ic_pause);


        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setWillNotDraw(false);

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        icon = new ImageView(context);

        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(icon, lp);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloader.isDownloaded(file)) {
                    cb.play(downloader.getDownloadedFile(file));
                } else {
                    download();
                }
            }
        });

        p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(Color.WHITE);
        strokeWidth = calc.dp(2);
        p.setStrokeWidth(strokeWidth + 1);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        rect = new RectF();

    }

    public static class Config {
        public static final int FINAL_ICON_EMPTY = -1;
        public final int finalIcon;
        public final boolean blue;
        public final boolean darkenBlue;
        final int sizeDp;

        public Config(int finalIcon, boolean blue, boolean darkenBlue, int size) {
            this.finalIcon = finalIcon;
            this.blue = blue;
            this.darkenBlue = darkenBlue;
            this.sizeDp = size;
        }
    }



    private Subscription subscription = Subscriptions.empty();

    private void download() {
        downloader.download(file);
        setProgress(0f);
        bind(file, cfg, cb);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(spec, spec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int radius = size / 2;
        canvas.drawCircle(radius, radius, radius, bgPaint);
        if (drawProgress) {
            canvas.drawArc(rect, 0f, 360f * progress, false, p);
        }
    }

    private void subscribeForFileDownload(final TdApi.FileEmpty file) {
        Observable<RxDownloadManager.FileState> request = downloader.nonMainThreadObservableFor(file);
        subscription = request
                .observeOn(mainThread())
                .subscribe(new Action1<RxDownloadManager.FileState>() {
                    @Override
                    public void call(RxDownloadManager.FileState fileState) {
                        if (fileState instanceof RxDownloadManager.FileDownloaded) {
                            final RxDownloadManager.FileDownloaded d = (RxDownloadManager.FileDownloaded) fileState;
                            cb.onFinished(d.f);
                            animateProgress(1f);
                            animator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (cfg.finalIcon == Config.FINAL_ICON_EMPTY) {
                                        animate()
                                                .alpha(0f);
                                    } else {
                                        bind(d.f, cfg, cb);
                                    }

                                }
                            });
                        } else {
                            RxDownloadManager.FileProgress p = (RxDownloadManager.FileProgress) fileState;
                            float progress = (float) p.p.ready / p.p.size;
                            cb.onProgress(p.p);
                            animateProgress(progress);
                        }
                    }
                });
    }

    ProgressProperty property = new ProgressProperty();

    private void animateProgress(float progress) {

        if (animator != null) {
            animator.cancel();
        }
        float diff = progress - getProgress();
        long duration = (long) (diff * 600);
        duration = Math.max(duration, 16*8);
        animator = ObjectAnimator.ofFloat(this, property, progress);
        animator.setInterpolator(INTERPOLATOR);
        animator.setDuration(duration);
        animator.start();

        //        setProgress(progress);
    }

    public void bind(TdApi.File f,Config cfg,  CallBack cb) {
        configure(cfg);
        this.cb = cb;
        clearAnimation();

        if (animator != null) {
            animator.cancel();
        }
        this.file = f;
        subscription.unsubscribe();
        invalidate();
        float alpha = 1f;

        if (downloader.isDownloaded(f)) {
            if (cfg.finalIcon == Config.FINAL_ICON_EMPTY){
                alpha = 0f;
            } else {
                icon.setImageLevel(LEVEL_EMPTY);
                if (cfg.darkenBlue){
                    bgPaint.setColor(BG_DARKEN_BLUE);
                }
                setEnabled(true);
                drawProgress = false;
                //show hight quality thumb
            }
            cb.onFinished(
                    downloader.getDownloadedFile(f));
        } else {
            TdApi.FileEmpty e = (TdApi.FileEmpty) f;
            if (downloader.isDownloading(e)) {
                icon.setImageLevel(LEVEL_PAUSE);
                setEnabled(false);
                subscribeForFileDownload(e);
                drawProgress = true;
                //show low quality thumb
            } else {
                icon.setImageLevel(LEVE_DOWNLOAD);
                setEnabled(true);
                drawProgress = false;
                //show hight quality thumb
            }
        }
        setAlpha(alpha);
    }

    private void configure(Config cfg) {
        if (this.cfg == cfg) return;
        requestLayout();
        size = calc.dp(cfg.sizeDp);
        if (size % 2 == 1) {
            size--;
        }
        rect.set(strokeWidth, strokeWidth, size - strokeWidth, size - strokeWidth);
        this.cfg = cfg;
        LevelListDrawable ls = new LevelListDrawable();
        Drawable icDownload = cfg.blue? downloadBlue: download;
        ls.addLevel(0, LEVE_DOWNLOAD, icDownload);
        Drawable icPause = cfg.blue? pauseBlue:ause;
        ls.addLevel(0, LEVEL_PAUSE, icPause);
        Resources res = getResources();
        if (cfg.finalIcon == Config.FINAL_ICON_EMPTY) {
            Drawable empty = res.getDrawable(R.drawable.empty);
            ls.addLevel(0, LEVEL_EMPTY, empty);
        } else {
            ls.addLevel(0, LEVEL_EMPTY, res.getDrawable(cfg.finalIcon));
        }
        icon.setImageDrawable(ls);
        if (cfg.blue){
            bgPaint.setColor(BG_BLUE);
            p.setColor(BG_DARKEN_BLUE);
        } else {
            bgPaint.setColor(BG_BLACK);
            p.setColor(Color.WHITE);
        }

        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;

        invalidate();
    }

    final class ProgressProperty extends Property<DownloadView, Float> {

        public ProgressProperty() {
            super(Float.class, "progress property");
        }

        @Override
        public void set(DownloadView object, Float value) {
            object.setProgress(value);
        }

        @Override
        public Float get(DownloadView object) {
            return object.getProgress();
        }
    }

    public interface CallBack {
        void onProgress(TdApi.UpdateFileProgress p);
        void onFinished(TdApi.FileLocal e);
        void play(TdApi.FileLocal e);
    }
}
