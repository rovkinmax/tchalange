package ru.korniltsev.telegram.profile;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.List;

public class ProfileAdapter extends BaseAdapter<ProfileAdapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_DATA = 1;
    final CallBack cb;
    public ProfileAdapter(Context ctx, CallBack cb) {
        super(ctx);
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_DATA;//super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = getViewFactory().inflate(R.layout.profile_header, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        } else {
            View view = getViewFactory().inflate(R.layout.profile_data, parent, false);
            return new VH(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position != 0) {
            final Item item = getItem(position);
            VH h = (VH) holder;
            if (item.icon == 0) {
                h.icon.setImageDrawable(null);
            } else {
                h.icon.setImageResource(item.icon);
            }
            h.data.setText(item.data);
            h.dataType.setText(item.localizedDataType);
            h.dataType.setClickable(item.bottomSheetActions != null);


        }
    }

    public  class VH extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView data;
        private final TextView dataType;

        public VH(View itemView) {
            super(itemView);
            icon = ((ImageView) itemView.findViewById(R.id.icon));
            data = ((TextView) itemView.findViewById(R.id.data));
            dataType = ((TextView) itemView.findViewById(R.id.data_type));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cb.clicked(
                            getItem(
                                    getAdapterPosition()));
                }
            });
        }
    }

    public static class Item {
        final int icon;
        final String data;
        final String localizedDataType;
        @Nullable final List<ListChoicePopup.Item> bottomSheetActions;

        public Item(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
            this.icon = icon;
            this.data = data;
            this.localizedDataType = localizedDataType;
            this.bottomSheetActions = bottomSheetActions;
        }
    }

    interface CallBack {
        void clicked(Item item);
    }
}
