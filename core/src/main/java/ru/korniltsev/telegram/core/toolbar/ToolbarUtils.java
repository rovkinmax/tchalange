package ru.korniltsev.telegram.core.toolbar;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import flow.Flow;
import flow.path.PathContext;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.utils.R;

/**
 * Created by korniltsev on 22/04/15.
 */
public class ToolbarUtils {
    public final Toolbar toolbar;
    final Context ctx;
    @Nullable private View customView;

    public ToolbarUtils(Context ctx, Toolbar toolbar) {
        this.ctx = ctx;
        this.toolbar = toolbar;
    }

    public static ToolbarUtils initToolbar(final View root) {
        Context ctx2 = root.getContext();
        Toolbar toolbar = (Toolbar) root.findViewById(R.id.toolbar);
        return new ToolbarUtils(ctx2, toolbar);
    }

    public ToolbarUtils setTitle(int res){
        toolbar.setTitle(res);
        return this;
    }

    public ToolbarUtils pop(){

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Flow.get(view.getContext())
                        .goBack();
            }
        };
        return pop(listener);
    }

    public ToolbarUtils pop(View.OnClickListener listener) {
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(listener);
        return this;
    }

    public ToolbarUtils setDrawer(DrawerLayout drawer, int openStr, int closeStr){
        Context ctx = drawer.getContext();
        ActivityOwner a = ObjectGraphService.getObjectGraph(ctx).get(ActivityOwner.class);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(a.expose(),  drawer, toolbar,
                openStr, closeStr
        );
        drawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        return this;
    }

    public ToolbarUtils addMenuItem(int menu, final int id, final Runnable runnable) {
        toolbar.inflateMenu(menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (id == menuItem.getItemId()) {
                    runnable.run();
                    return true;
                }
                return false;
            }
        });
        return this;
    }

    public ToolbarUtils inflate(int menuRes) {
        toolbar.inflateMenu(menuRes);
        return this;
    }

    public ToolbarUtils setMenuClickListener(Toolbar.OnMenuItemClickListener l){
        toolbar.setOnMenuItemClickListener(l);
        return this;
    }

    public void hideMenu(int id) {
        toolbar.getMenu()
                .findItem(id)
                .setVisible(false);
    }

    public void showMenu(int id) {
        toolbar.getMenu()
                .findItem(id)
                .setVisible(true);
    }

    public void setIcon(Bitmap bitmap) {
        toolbar.setNavigationIcon(new BitmapDrawable(bitmap));
        //todo заменить на кастом вью
        //todo fade
    }

    public ToolbarUtils customView(int layout) {
        customView = LayoutInflater.from(ctx).inflate(layout, toolbar, false);
        toolbar.addView(customView);
        return this;
    }

    @Nullable
    public View getCustomView() {
        return customView;
    }




}
