package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;

public class GeoPointVH extends BaseVH {

    private final GeoPointView map;

    public GeoPointVH(View itemView, Adapter adapter) {
        super(itemView, adapter);
        map = (GeoPointView) itemView.findViewById(R.id.geo_point);
    }

    @Override
    public void bind(TdApi.Message item) {
        super.bind(item);
        map.set((TdApi.MessageGeoPoint) item.message);
    }
}
