package ru.korniltsev.telegram.attach_panel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;
import dagger.ObjectGraph;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import rx.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.core.Utils.exactly;

public abstract class AttachPanelPopup extends PopupWindow {

    private static final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
    public static final int DURATION = 256;

//    final Callback callback;
    protected final DpCalculator dpCalc;
    private RecyclerView recentGalleryImages;
    private View outside;
    protected RxGlide rxGlide;
//    private RecentImagesAdapter adapter;
//    private TextView btnTakePhoto;
//    private TextView btnChooseFromGallery;
    protected ViewGroup panel;

    public AttachPanelPopup(Context ctx) {
        super(LayoutInflater.from(ctx)
                .inflate(R.layout.attach_panel_view_attach_panel, null, false));

        final ViewGroup view = (ViewGroup) getContentView();
        ObjectGraph objectGraph = ObjectGraphService.getObjectGraph(ctx);
        rxGlide = objectGraph.get(RxGlide.class);
        dpCalc = objectGraph.get(DpCalculator.class);

        final Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        setWidth(exactly(display.getWidth()));
        setHeight(exactly(display.getHeight()));

        inflatePanel(view);
        outside = view.findViewById(R.id.outside);
        outside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        panel = (ViewGroup) view.findViewById(R.id.panel);
//        initView();
    }

    protected abstract void inflatePanel(ViewGroup view);


    protected abstract void initView();

    void show(Activity ctx) {
        int marginBottom = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//tdo wtf
            marginBottom = getNavBarHeight(ctx);
        }

        showAtLocation(ctx.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.LEFT, 0, marginBottom);
    }

    public interface Callback {
        void sendImages(List<String> selecteImages);

        void chooseFromGallery();

        void takePhoto();
    }

    public static int getNavBarHeight(Context ctx) {
        Resources resources = ctx.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    public void showAtLocation(final View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        panel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                outside.setAlpha(0);
                panel.setTranslationY(panel.getHeight());
                panel.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (dissmised){
                            return;
                        }
                        outside.animate()
                                .setInterpolator(decelerateInterpolator)
//                                .setDuration(DURATION)
                                .alpha(0.32f);

                        panel.animate()
                                .setInterpolator(decelerateInterpolator)
                                .setDuration(DURATION)
                                .translationY(0);
                    }
                }, 32);
                ViewTreeObserver o = panel.getViewTreeObserver();
                if (o.isAlive()) {
                    o.removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    boolean dissmised = false;

    @Override
    public void dismiss() {
        if (dissmised) {
            return;
        }
        outside.clearAnimation();
        panel.clearAnimation();
        dissmised = true;
        outside.animate()
//                .setInterpolator(decelerateInterpolator)
                .setDuration(DURATION)
                .alpha(0f)
        .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                realDismiss();
            }
        });
        panel.animate()
                .setInterpolator(decelerateInterpolator)
                .setDuration(DURATION)
                .translationY(panel.getHeight());
    }
    public void realDismiss() {
        super.dismiss();
    }
}
