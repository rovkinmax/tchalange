package ru.korniltsev.telegram.core.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    //todo split the view
    //todo make it not responsible for downloading - only displaying progress, icons, bg
    public static final int LEVE_DOWNLOAD = 0;
    public static final int LEVEL_DOWNLOAD_PAUSE = 1;
    public static final int LEVEL_PLAY = 2;
    public static final int LEVEL_PAUSE = 3;
    public static final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();
    public static final int BG_BLUE = 0xffecf4F9;
    public static final int BG_BLACK = 0xB1000000;
    public static final int BG_DARKEN_BLUE = 0xff579cda;
    public static final int DARKEN_BLUE_DURATION = 208;
    public static final int FADE_PROGRESS_DURATION = 208;
    public static final int SCALE_DOWN_DURATION = 80;
    public static final int SCALE_UP_DURAION = 80;

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
    private final OnClickListener clicker;

    float progress;

    @Inject RxDownloadManager downloader;
    @Inject DpCalculator calc;
    private TdApi.File file;
    private boolean drawProgress;
    private ObjectAnimator progressAnimator;
    private ObjectAnimator progressAlphaAnimator;
    private CallBack cb;
    private Config cfg;
    private int size;
    private View clickTarget;
    private AnimatorSet currentAnimation;
    private ObjectAnimator bgColorAnimator;

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

        clicker = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloader.isDownloaded(file)) {
                    cb.play(downloader.getDownloadedFile(file));
                } else if (downloader.isDownloading((TdApi.FileEmpty) file)){
                    //do nothing
                } else {
                    download();
                }
            }
        };
//        setOnClickListener(clicker);

        p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(Color.WHITE);
        strokeWidth = calc.dp(2);
        p.setStrokeWidth(strokeWidth + 1);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        rect = new RectF();

    }

    public Float getProgressAlpha() {
        float alpha = p.getAlpha();
        return alpha/255;
    }

    public Integer getBgColor() {
        return bgPaint.getColor();
    }

    public static class Config {
        public static final int FINAL_ICON_EMPTY = -1;
        public final int playIconIcon;
        public final int pauseIconIcon;
        public final boolean blue;
        public final boolean darkenBlue;
        final int sizeDp;

        public Config(int finalIcon, int pauseIconIcon, boolean blue, boolean darkenBlue, int size) {
            this.playIconIcon = finalIcon;
            this.pauseIconIcon = pauseIconIcon;
            this.blue = blue;
            this.darkenBlue = darkenBlue;
            this.sizeDp = size;
        }
    }



    private Subscription subscription = Subscriptions.empty();

    private void download() {
        downloader.download(file);
        setProgress(0f);
        bind(file, cfg, cb, clickTarget, true);
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
                            cb.onFinished(d.f, true);
                            animateProgress(1f);
                            progressAnimator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (cfg.playIconIcon == Config.FINAL_ICON_EMPTY) {
                                        animate()
                                                .alpha(0f);
                                    } else {
                                        bind(d.f, cfg, cb, clickTarget, true);
                                        fadeProgress();
                                        if (cfg.darkenBlue){
                                            animateDarkenBlue();
                                        }
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

    private void animateDarkenBlue() {
        bgPaint.setColor(BG_BLUE);
        bgColorAnimator =  ObjectAnimator.ofInt(this, bgColorProperty, BG_DARKEN_BLUE);
        bgColorAnimator.setEvaluator(ArgbEvaluator.getInstance());
        bgColorAnimator.setDuration(DARKEN_BLUE_DURATION);
        bgColorAnimator.start();
    }

    private void fadeProgress() {
        if (progressAlphaAnimator != null){
            progressAlphaAnimator.cancel();
        }
        drawProgress = true;
        progressAlphaAnimator = ObjectAnimator.ofFloat(this, progressAlphaProperty, 0)
                .setDuration(FADE_PROGRESS_DURATION);
        progressAlphaAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawProgress = false;
                invalidate();
            }
        });
        progressAlphaAnimator.start();
    }

    ProgressProperty property = new ProgressProperty();
    ProgressAlphaProperty progressAlphaProperty = new ProgressAlphaProperty();
    BgColorProperty bgColorProperty = new BgColorProperty();

    float animationProgress = 0f;
    private void animateProgress(final float progress) {
        animationProgress = progress;
        if (progressAnimator != null && progressAnimator.isRunning()) {
            return;
            //            progressAnimator.cancel();
        }
        float diff = progress - getProgress();
        long duration = 512;//(long) (diff * 600);
//        long duration = (long) (diff * 600);
//        duration = Math.max(duration, 16 * 8);
        progressAnimator = ObjectAnimator.ofFloat(this, property, progress);
        progressAnimator.setInterpolator(INTERPOLATOR);
        progressAnimator.setDuration(duration);
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (DownloadView.this.progress != animationProgress) {
                    animateProgress(animationProgress);
                }
            }
        });
        progressAnimator.start();

        //        setProgress(progress);
    }
    public void bind(TdApi.File f,Config cfg,  CallBack cb, View clickTarget) {
        bind(f, cfg, cb, clickTarget, false);
    }
    private void bind(TdApi.File f,Config cfg,  CallBack cb, View clickTarget, boolean animateIcons) {
        p.setAlpha(255);
        this.clickTarget = clickTarget;
        clickTarget.setOnClickListener(clicker);
        configure(cfg);
        this.cb = cb;
        clearAnimation();

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (progressAlphaAnimator != null) {
            progressAlphaAnimator.cancel();
        }
        if (bgColorAnimator != null){
            bgColorAnimator.cancel();
        }
        this.file = f;
        subscription.unsubscribe();
        invalidate();
        float alpha = 1f;

        if (downloader.isDownloaded(f)) {
            if (cfg.playIconIcon == Config.FINAL_ICON_EMPTY) {
                alpha = 0f;
            } else {
                setLevel(LEVEL_PLAY, animateIcons);
                if (cfg.darkenBlue) {
                    bgPaint.setColor(BG_DARKEN_BLUE);
                }
                setEnabled(true);
                drawProgress = false;
                //show hight quality thumb
            }
            cb.onFinished(
                    downloader.getDownloadedFile(f), false);
        } else {
            TdApi.FileEmpty e = (TdApi.FileEmpty) f;
            if (downloader.isDownloading(e)) {
                setLevel(LEVEL_DOWNLOAD_PAUSE, animateIcons);
                setEnabled(false);
                subscribeForFileDownload(e);
                drawProgress = true;
                //show low quality thumb
            } else {
                setLevel(LEVE_DOWNLOAD, animateIcons);
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
        int shift = strokeWidth - 1;
        rect.set(shift, shift, size - shift, size - shift);
        this.cfg = cfg;
        LevelListDrawable ls = new LevelListDrawable();
        Drawable icDownload = cfg.blue? downloadBlue: download;
        ls.addLevel(0, LEVE_DOWNLOAD, icDownload);
        Drawable icPause = cfg.blue? pauseBlue:ause;
        ls.addLevel(0, LEVEL_DOWNLOAD_PAUSE, icPause);
        Resources res = getResources();
        if (cfg.playIconIcon == Config.FINAL_ICON_EMPTY) {
            Drawable empty = res.getDrawable(R.drawable.empty);
            ls.addLevel(0, LEVEL_PLAY, empty);
        } else {
            ls.addLevel(0, LEVEL_PLAY, res.getDrawable(cfg.playIconIcon));
        }
        if (cfg.pauseIconIcon == Config.FINAL_ICON_EMPTY){
            Drawable empty = res.getDrawable(R.drawable.empty);
            ls.addLevel(0, LEVEL_PAUSE, empty);
        } else {
            ls.addLevel(0, LEVEL_PAUSE, res.getDrawable(cfg.pauseIconIcon));
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
    AnimatorSet s = new AnimatorSet();
    public void setLevel(final int level, boolean animate) {
        if (currentAnimation != null){
            currentAnimation.cancel();
        }
        if (!animate) {
            icon.setImageLevel(level);
            return;
        }

        AnimatorSet scaleDown = new AnimatorSet()
                .setDuration(SCALE_DOWN_DURATION);
//        icon.setScaleX(1);
//        icon.setScaleY(1);
        scaleDown.playTogether(
                ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 0.1f),
                ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 0.1f))
        ;
        scaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                icon.setImageLevel(level);
            }
        });
        AnimatorSet scaleUp = new AnimatorSet()
                .setDuration(SCALE_UP_DURAION);
        scaleUp.playTogether(
                ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.1f, 1f),
                ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.1f, 1f));
        currentAnimation = new AnimatorSet();
        currentAnimation.playSequentially(scaleDown, scaleUp);
        currentAnimation.start();
//        scaleDownX.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                icon.setImageLevel(level);
//            }
//        });
//        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1);
//        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1);

        //        /s.addListener();
//        icon.setImageLevel(level);
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

    final class ProgressAlphaProperty extends Property<DownloadView, Float> {

        public ProgressAlphaProperty() {
            super(Float.class, "progress alpha property");
        }

        @Override
        public void set(DownloadView object, Float value) {
            object.setProgressAlpha(value);
        }

        @Override
        public Float get(DownloadView object) {
            return object.getProgressAlpha();
        }
    }

    private void setProgressAlpha(Float value) {
        p.setAlpha((int) (255 * value));
        invalidate();

//        ObjectAnimator.ofArg
    }

    final class BgColorProperty extends Property<DownloadView, Integer>{
        public BgColorProperty() {
            super(Integer.class, "progress alpha property");
        }

        @Override
        public void set(DownloadView object, Integer value) {
            object.setBgColor(value);
        }

        @Override
        public Integer get(DownloadView object) {
            return object.getBgColor();
        }
    }

    private void setBgColor(Integer value) {
        bgPaint.setColor(value);
        invalidate();
    }

    public static abstract class  CallBack {
        public void onProgress(TdApi.UpdateFileProgress p){}
        public void onFinished(TdApi.FileLocal e, boolean justDownloaded){}
        public void play(TdApi.FileLocal e){}
    }
}
