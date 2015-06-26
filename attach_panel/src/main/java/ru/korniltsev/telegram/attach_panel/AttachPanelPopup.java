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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;
import dagger.ObjectGraph;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import rx.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.core.Utils.exactly;

public class AttachPanelPopup extends PopupWindow {

    final Callback callback;

    private final DpCalculator dpCalc;
    private final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
    private RecyclerView recentGalleryImages;
    private View outside;
    private RxGlide rxGlide;
    private RecentImagesAdapter adapter;
    private TextView btnTakePhoto;
    private TextView btnChooseFromGallery;
    private View panel;

    public AttachPanelPopup(View view, Callback callback) {
        super(view);
        this.callback = callback;
        //        ObjectGraphService.inject();

        Context ctx = view.getContext();
        ObjectGraph objectGraph = ObjectGraphService.getObjectGraph(ctx);
        rxGlide = objectGraph.get(RxGlide.class);
        dpCalc = objectGraph.get(DpCalculator.class);

        final Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        setWidth(exactly(display.getWidth()));
        setHeight(exactly(display.getHeight()));

        initView(view);
    }

    private void initView(View view) {
        Context ctx = view.getContext();
        adapter = new RecentImagesAdapter(ctx, rxGlide, new RecentImagesAdapter.Callback() {
            @Override
            public void imagesSelected(int count) {
                updateButtonText(count);
            }
        });
        btnTakePhoto = (TextView) view.findViewById(R.id.btn_take_photo);
        btnChooseFromGallery = (TextView) view.findViewById(R.id.btn_choose_from_gallery);
        recentGalleryImages = ((RecyclerView) view.findViewById(R.id.recent_images));
        recentGalleryImages.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
        recentGalleryImages.addItemDecoration(new InsetDecorator());
        recentGalleryImages.setAdapter(adapter);
        outside = view.findViewById(R.id.outside);
        outside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        panel = view.findViewById(R.id.panel);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.takePhoto();
            }
        });
        btnChooseFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> selectedImages = adapter.getSelectedImages();
                if (selectedImages.size() == 0) {
                    callback.chooseFromGallery();
                } else {
                    callback.sendImages(selectedImages);
                }
            }
        });

        GalleryService.recentImages(ctx)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<List<String>>() {
                    @Override
                    public void onNext(List<String> response) {
                        adapter.addAll(response);
                    }
                });
    }

    private void updateButtonText(int count) {
        Resources res = getContentView().getContext().getResources();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (count == 0) {
            sb.append(res.getString(R.string.choose_from_gallery));
        } else {
            SpannableString nPhotos = new SpannableString(res.getQuantityString(R.plurals.n_photos, count, count));
            nPhotos.setSpan(new StyleSpan(Typeface.BOLD), 0, nPhotos.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            sb.append(res.getString(R.string.send))
                    .append(" ");
            sb.append(nPhotos);
            //            btnChooseFromGallery.setText(res.getString(R.string.send) + " " + nPhotos);
        }
        btnChooseFromGallery.setText(sb);
    }

    public static AttachPanelPopup create(Activity ctx, Callback callback) {
        LayoutInflater viewFactory = LayoutInflater.from(ctx);
        View view = viewFactory.inflate(R.layout.view_attach_panel, null, false);

        AttachPanelPopup res = new AttachPanelPopup(view, callback);

        int marginBottom = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//tdo wtf
            marginBottom = getNavBarHeight(ctx);
        }

        res.showAtLocation(ctx.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.LEFT, 0, marginBottom);

        return res;
    }

    private class InsetDecorator extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int dip8 = dpCalc.dp(8);
            int position = parent.getChildViewHolder(view)
                    .getPosition();

            if (position == 0) {
                outRect.left = dip8;
                outRect.right = dip8 / 2;
            } else if (position == adapter.getItemCount() - 1) {
                outRect.right = dip8;
                outRect.left = dip8 / 2;
            } else {
                outRect.left = dip8 / 2;
                outRect.right = dip8 / 2;
            }
        }
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
                outside.animate()
                        .setInterpolator(decelerateInterpolator)
                        .setDuration(128)
                        .alpha(0.5f);
                panel.setTranslationY(panel.getHeight());
                panel.animate()
                        .setInterpolator(decelerateInterpolator)
                        .setDuration(128)
                        .translationY(0);
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
        dissmised = true;
        outside.animate()
                .setInterpolator(decelerateInterpolator)
                .setDuration(256)
                .alpha(0f)
        .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                realDismiss();
            }
        });
        panel.animate()
                .setInterpolator(decelerateInterpolator)
                .setDuration(128)
                .translationY(panel.getHeight());
    }
    public void realDismiss() {
        super.dismiss();
    }
}
