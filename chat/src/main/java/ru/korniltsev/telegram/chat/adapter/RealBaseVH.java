package ru.korniltsev.telegram.chat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;

public abstract class RealBaseVH extends RecyclerView.ViewHolder {
    public final Adapter adapter;
    public RealBaseVH(View itemView, Adapter adapter) {
        super(itemView);
        this.adapter = adapter;
    }

    public abstract void bind(TdApi.Message item);
}
