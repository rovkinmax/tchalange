package ru.korniltsev.telegram.core.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;

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

    public void addFirst(List<T> ms) {
        ts.addAll(0, ms);
        notifyItemRangeInserted(0, ms.size());
    }

    public void addFirst(T m) {
        ts.add(0, m);
        notifyItemInserted(0);
    }

    public List<T> getTs() {
        return ts;
    }

    public T getLast() {
        return ts.get(ts.size() - 1);
    }

    public void setData(List<T> chats) {
        checkMainThread();
        ts.clear();
        ts.addAll(chats);
        notifyDataSetChanged();
    }
}