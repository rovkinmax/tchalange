package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.view.PhotoMessageView;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.RxPicasso;
import ru.korniltsev.telegram.core.views.AvatarView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Adapter extends BaseAdapter<TdApi.Message, Adapter.BaseVH> {
    public static final int VIEW_TYPE_PHOTO = 0;
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_STICKER = 2;
    private Map<Integer, TdApi.User> users = new HashMap<>();
    final RxPicasso picasso;
    public Adapter(Context ctx, RxPicasso picasso) {
        super(ctx);
        this.picasso = picasso;
    }

    @Override
    public int getItemViewType(int position) {
        TdApi.MessageContent message = getItem(position).message;
        if (message instanceof TdApi.MessagePhoto){
            return VIEW_TYPE_PHOTO;
        } else if (message instanceof TdApi.MessageSticker){
            return VIEW_TYPE_STICKER;
        }
        return VIEW_TYPE_TEXT;
    }

    @Override
    public BaseVH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PHOTO){
            View view = getViewFactory()
                    .inflate(R.layout.item_photo, parent, false);
            return new PhotoMessageVH(view);
        } else if (viewType == VIEW_TYPE_STICKER){
            View view = getViewFactory()
                    .inflate(R.layout.item_sticker, parent, false);
            return new StickerVH(view);
        } else {
            View view = getViewFactory()
                    .inflate(R.layout.item_message, parent, false);
            return new TextMessageVH(view);
        }
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

    public Portion getPortion() {
        return new Portion(getData(), users);
    }

    class PhotoMessageVH extends BaseVH {
        private final PhotoMessageView image;

        public PhotoMessageVH(View itemView) {
            super(itemView);
            image = (PhotoMessageView) itemView.findViewById(R.id.image);
        }

        @Override
        public void bind(TdApi.Message item) {
            super.bind(item);

            TdApi.MessagePhoto photo = (TdApi.MessagePhoto) item.message;
            image.load(photo);
        }
    }

    class StickerVH extends BaseVH {
        final ImageView image;
        public StickerVH(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
        }

        @Override
        public void bind(TdApi.Message item) {
            super.bind(item);
            TdApi.MessageSticker sticker = (TdApi.MessageSticker) item.message;
            //todo thumb
            picasso.loadPhoto(sticker.sticker.sticker)
                    .into(image);
        }
    }
    class TextMessageVH extends BaseVH {

        private final TextView message;

        public TextMessageVH(View itemView) {
            super(itemView);
            message = ((TextView) itemView.findViewById(R.id.message));



        }

        @Override
        public void bind(TdApi.Message item) {
            super.bind(item);
            TdApi.MessageContent msg = item.message;
            if (msg instanceof TdApi.MessageText) {//todo get rid of this "if"
                String text = ((TdApi.MessageText) msg).text;
                message.setText(text);
            } else {
                message.setText("");
            }


        }
    }
    abstract class BaseVH  extends RecyclerView.ViewHolder{
        private final AvatarView avatar;
        private final TextView nick;
        private final TextView time;

        public BaseVH(View itemView) {
            super(itemView);
            avatar = (AvatarView) itemView.findViewById(R.id.avatar);
            nick = ((TextView) itemView.findViewById(R.id.nick));
            time = (TextView) itemView.findViewById(R.id.time);

            //todo blue dot
            //todo message set status
        }

        public void bind(TdApi.Message item){
            TdApi.User user = users.get(item.fromId);
            avatar.loadAvatarFor(user);
            String name = name(user);
            nick.setText(name);
            long timeInMillis = item.date * 1000;
            time.setText(MESSAGE_TIME_FORMAT.format(timeInMillis));
        }
    }

    private String name(TdApi.User user) {//todo
        String name;
        StringBuilder sb = new StringBuilder();
        if (user.firstName.length() != 0) {
            sb.append(user.firstName);
        }
        if (user.lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(user.lastName);
        }
        name = sb.toString();
        return name;
    }

    private final SimpleDateFormat MESSAGE_TIME_FORMAT = new SimpleDateFormat("K:mm a", Locale.getDefault());

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

    public Map<Integer, TdApi.User> getUsers() {
        return users;
    }
}
