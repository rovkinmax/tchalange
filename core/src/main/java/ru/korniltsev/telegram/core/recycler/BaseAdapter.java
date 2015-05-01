package ru.korniltsev.telegram.core.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private Context ctx;
    private final List<T> ts;
    private final LayoutInflater viewFactory;

    public BaseAdapter(Context ctx) {
        this(ctx, new ArrayList<T>());
    }
    public BaseAdapter(Context ctx, List<T> ts) {
        this.ctx = ctx;
        this.ts = ts;
        this.viewFactory = LayoutInflater.from(ctx);
    }

    @Override
    public final int getItemCount() {
        return ts.size();
    }

    public final T getItem(int pos) {
        return ts.get(pos);
    }

    public final LayoutInflater getViewFactory() {
        return viewFactory;
    }

    public void addAll(T[] newTs) {
        int start = ts.size();
        for (T t : newTs) {
            ts.add(t);
        }
        notifyItemRangeInserted(start, newTs.length);
    }

    public void addAll(List<T> newTs) {
        int start = ts.size();
        for (T t : newTs) {
            ts.add(t);
        }
        notifyItemRangeInserted(start, newTs.size());
    }

    public List<T> getData() {
        return ts;
    }

    public Context getCtx() {
        return ctx;
    }

    public void clearData() {
        int size = ts.size();
        ts.clear();
        notifyItemRangeRemoved(0, size);
    }
}