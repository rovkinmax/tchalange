package ru.korniltsev.telegram.attach_panel;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.List;

public class RecentImagesBottomSheet extends AttachPanelPopup {
    private final Callback callback;
    private TextView btnTakePhoto;
    private TextView btnChooseFromGallery;
    private RecyclerView recentGalleryImages;
    private RecentImagesAdapter adapter;

    RecentImagesBottomSheet(Context ctx, Callback callback) {
        super(ctx);
        this.callback = callback;
        initView();

    }

    public static AttachPanelPopup create(Activity ctx, Callback callback) {
        AttachPanelPopup res = new RecentImagesBottomSheet(ctx, callback);
        res.show(ctx);
        return res;
    }

    private class InsetDecorator extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int dip8 = dpCalc.dp(8);
            int position = parent.getChildViewHolder(view)
                    .getAdapterPosition();

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

    @Override
    protected void inflatePanel(ViewGroup view) {
        LayoutInflater.from(view.getContext())
                .inflate(R.layout.attach_panel_panel, view, true);
    }

    @Override
    protected void initView() {
        final View view = getContentView();
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
}
