package ru.korniltsev.telegram.core.recycler;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class EndlessOnScrollListener extends RecyclerView.OnScrollListener {
    final boolean waitForLastItem;
    final LinearLayoutManager lm;
    final RecyclerView.Adapter a;
    final Runnable run;

    public EndlessOnScrollListener(LinearLayoutManager lm, RecyclerView.Adapter a, boolean waitForLastItem, Runnable run) {
        this.lm = lm;
        this.a = a;
        this.waitForLastItem = waitForLastItem;
        this.run = run;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (waitForLastItem){
            if (lm.findLastVisibleItemPosition() == a.getItemCount() - 1) {
                run.run();
            }
        } else {
            if (lm.findFirstVisibleItemPosition() == 0) {
                run.run();
            }
        }

    }
}