package ru.korniltsev.telegram.core.flow;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by korniltsev on 22/04/15.
 */
public class FlowLike {
    public static final int ROOT = android.R.id.content;
    public static final String TAG = "FlowLike";
    private final FragmentActivity ctx;
    private final FragmentManager fm;
    private Bundle savedInstanceState;

    public FlowLike(FragmentActivity ctx, Bundle savedInstanceState) {
        this.ctx = ctx;
        fm = ctx.getSupportFragmentManager();
        this.savedInstanceState = savedInstanceState;
    }


    public static FlowLike from(Context ctx) {
        return ((FlowLikeActivity) ctx).flow;
    }





    public void push(Fragment f, String tag) {
        ctx.getSupportFragmentManager()
                .beginTransaction()
                .replace(ROOT, f, tag)
                .addToBackStack(tag)
                .commit();
    }

    public FlowLike clearStack() {
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        return this;
    }


    public void pop() {
        pop(null);
    }

    public void pop(Object result) {
        int fsCount = fm.getBackStackEntryCount();
        if (fsCount == 1) {
            ctx.finish();
            assertNull(result);
            return;
        }
        boolean popped = fm.popBackStackImmediate();
        assertTrue(popped);
        fsCount--;
        if (result != null) {
            FragmentManager.BackStackEntry e = fm.getBackStackEntryAt(fsCount - 1 );
            Log.d(TAG, String.format("%s %d %s", e.getName(), e.getId(), e.getBreadCrumbTitle()));
            BaseFragment f = (BaseFragment) fm.findFragmentByTag(e.getName());
            f.onResult(result);
        }
    }
}
