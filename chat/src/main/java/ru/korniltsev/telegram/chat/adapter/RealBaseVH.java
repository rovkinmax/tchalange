package ru.korniltsev.telegram.chat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;

import static junit.framework.Assert.assertTrue;

public abstract class RealBaseVH extends RecyclerView.ViewHolder {
    public final Adapter adapter;
    public RealBaseVH(View itemView, Adapter adapter) {
        super(itemView);
        this.adapter = adapter;
    }

    public abstract void bind(TdApi.Message item);

    public String getNameForSenderOf(TdApi.Message item) {
        int fromId = item.fromId;
        assertTrue(fromId != 0);
        TdApi.User user = adapter.getUserHolder().getUser(fromId);
        return Utils.uiName(user);
    }
}
