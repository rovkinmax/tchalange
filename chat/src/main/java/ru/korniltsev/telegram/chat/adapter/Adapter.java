package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.RxPicasso;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Adapter extends BaseAdapter<TdApi.Message, BaseVH> {

    public static final int VIEW_TYPE_PHOTO = 0;
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_STICKER = 2;
    public static final int VIEW_TYPE_AUDIO = 3;
    public static final int VIEW_TYPE_GEO = 4;
    final Map<Integer, TdApi.User> users = new HashMap<>();
    final RxPicasso picasso;

    public Adapter(Context ctx, RxPicasso picasso) {
        super(ctx);
        this.picasso = picasso;
    }

    @Override
    public int getItemViewType(int position) {
        TdApi.MessageContent message = getItem(position).message;
        if (message instanceof TdApi.MessagePhoto) {
            return VIEW_TYPE_PHOTO;
        } else if (message instanceof TdApi.MessageSticker) {
            return VIEW_TYPE_STICKER;
        } else if (message instanceof TdApi.MessageAudio) {
            return VIEW_TYPE_AUDIO;
        } else if (message instanceof TdApi.MessageGeoPoint) {
            return VIEW_TYPE_GEO;
        }
        return VIEW_TYPE_TEXT;
    }

    @Override
    public BaseVH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PHOTO) {
            View view = getViewFactory()
                    .inflate(R.layout.item_photo, parent, false);
            return new PhotoMessageVH(view, this);
        } else if (viewType == VIEW_TYPE_STICKER) {
            View view = getViewFactory()
                    .inflate(R.layout.item_sticker, parent, false);
            return new StickerVH(view, this);
        } else if (viewType == VIEW_TYPE_AUDIO){
            View view = getViewFactory()
                    .inflate(R.layout.item_audio, parent, false);
            return new AudioVH(view, this);
        } else if (viewType == VIEW_TYPE_GEO){
            View view = getViewFactory()
                    .inflate(R.layout.item_geo, parent, false);
            return new GeoPointVH(view, this);
        }
        View view = getViewFactory()
                .inflate(R.layout.item_message, parent, false);
        return new TextMessageVH(view, this);
    }

    @Override
    public void onBindViewHolder(BaseVH holder, int position) {
        TdApi.Message item = getItem(position);
        holder.bind(item);
    }

    public void addHistory(Portion ms) {
        addAll(ms.ms);
        users.putAll(ms.us);
    }

    public void insertNewMessage(Portion portion) {
        addFirst(portion.ms);
        users.putAll(portion.us);
    }

    public void updateMessageId(TdApi.UpdateMessageId upd) {
        List<TdApi.Message> ts = getTs();
        for (int i = 0; i < ts.size(); i++) {
            TdApi.Message message = ts.get(i);
            if (message.id == upd.oldId) {
                message.id = upd.newId;
                notifyItemChanged(i);
                return;
            }
        }
    }

    public Map<Integer, TdApi.User> getUsers() {
        return users;
    }

    public Portion getPortion() {
        return new Portion(getData(), users);
    }

    public static class Portion {
        public final List<TdApi.Message> ms;
        public final Map<Integer, TdApi.User> us;

        public Portion(List<TdApi.Message> ms, List<TdApi.User> us) {
            this.ms = ms;
            this.us = new HashMap<>();
            for (TdApi.User u : us) {
                this.us.put(u.id, u);
            }
        }

        public Portion(List<TdApi.Message> ms, Map<Integer, TdApi.User> us) {
            this.ms = ms;
            this.us = us;
        }

        public Portion(TdApi.Message msg) {
            this.ms = Collections.singletonList(msg);
            this.us = Collections.emptyMap();
        }
    }
}
