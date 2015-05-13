package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;
import ru.korniltsev.telegram.core.rx.RxChat;

public class GeoPointVH extends BaseAvatarVH {

    private final GeoPointView map;

    public GeoPointVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        map = (GeoPointView) itemView.findViewById(R.id.geo_point);
    }

    @Override
    public void bind(RxChat.ChatListItem item) {
        super.bind(item);
        TdApi.Message msg = ((RxChat.MessageItem) item).msg;
        map.set((TdApi.MessageGeoPoint) msg.message);
    }
}
