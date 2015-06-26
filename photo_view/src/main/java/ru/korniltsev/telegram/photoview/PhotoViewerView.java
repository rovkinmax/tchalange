package ru.korniltsev.telegram.photoview;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

import javax.inject.Inject;

public class PhotoViewerView extends FrameLayout {
    @Inject PhotoViewer.Presenter presenter;
    @Inject RxGlide picasso;
    @Inject ActivityOwner activity;
    private PhotoView imageView;
    private ToolbarUtils toolbar;
    //    private Target t = new Target() {
    //        @Override
    //        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    //
    //        }
    //
    //        @Override
    //        public void onBitmapFailed(Drawable errorDrawable) {
    //
    //        }
    //
    //        @Override
    //        public void onPrepareLoad(Drawable placeHolderDrawable) {
    //
    //        }
    //    }

    public PhotoViewerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.photo_viewer)
                .setMenuClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (R.id.menu_delete == id){
                            presenter.deleteMessage();
                            return true;
                        } else if (R.id.menu_save_to_gallery == id){
                            presenter.saveToGallery();
                        }
                        return true;
                    }
                })
                .pop();
        imageView = ((PhotoView) findViewById(R.id.image));

        activity.setStatusBarColor(Color.BLACK);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void show(TdApi.File f) {
        picasso.loadPhoto(f, false)
                .into(this.imageView);
    }

    public void hideDeleteMessageMenuItem() {
        toolbar.hideMenu(R.id.menu_delete);
    }
}
