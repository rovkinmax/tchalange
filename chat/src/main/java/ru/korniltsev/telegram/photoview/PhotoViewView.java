package ru.korniltsev.telegram.photoview;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

public class PhotoViewView extends FrameLayout {
    @Inject PhotoViewPresenter presenter;
    @Inject RxGlide picasso;
    @Inject ActivityOwner activity;
    private uk.co.senab.photoview.PhotoView imageView;
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

    public PhotoViewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.photo_view)
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
        imageView = ((uk.co.senab.photoview.PhotoView) findViewById(R.id.image));

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
