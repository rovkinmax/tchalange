package ru.korniltsev.telegram.profile;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

public class ProfileAdapter extends BaseAdapter<ProfileAdapter.Item, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_DATA = 1;
    private static final int VIEW_TYPE_DATA_HORIZONTAL = 2;
    private static final int VIEW_TYPE_DIVIDER = 3;

    final CallBack cb;

    public ProfileAdapter(Context ctx, CallBack cb) {
        super(ctx);
        this.cb = cb;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            Item item = getItem(position);
            return getTypeByItem(item);
        }
    }

    private int getTypeByItem(Item item) {
        if (item.lastInGroup) {
            return VIEW_TYPE_DIVIDER;
        } else {
            return getTypeForNotEmptyItem(item);
        }
    }

    private int getTypeForNotEmptyItem(Item item) {
        if (item.getClass() == HorizontalItem.class) {
            return VIEW_TYPE_DATA_HORIZONTAL;
        } else {
            return VIEW_TYPE_DATA;
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                View view = getViewFactory().inflate(R.layout.profile_header, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }
            case VIEW_TYPE_DATA: {
                View view = getViewFactory().inflate(R.layout.profile_data, parent, false);
                return new VH(view);
            }

            case VIEW_TYPE_DATA_HORIZONTAL: {
                View view = getViewFactory().inflate(R.layout.profile_data_horisontal, parent, false);
                return new VH(view);
            }

            case VIEW_TYPE_DIVIDER: {
                View view = getViewFactory().inflate(R.layout.profile_divider, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }
        }
        return new RecyclerView.ViewHolder(new View(getViewFactory().getContext())) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_DATA) {
            bindViewData((VH) holder, position);
        } else {
            if (holder.getItemViewType() == VIEW_TYPE_DATA_HORIZONTAL) {
                bindViewDataHorizontal((VH) holder, position);
            }
        }
    }

    private void bindViewDataHorizontal(VH holder, int position) {
        bindViewData(holder, position);
    }

    private void bindViewData(VH holder, int position) {
        final Item item = getItem(position);
        if (item.icon == 0) {
            holder.icon.setImageDrawable(null);
        } else {
            holder.icon.setImageResource(item.icon);
        }
        holder.data.setText(item.data);
        holder.dataType.setText(item.localizedDataType);
        holder.dataType.setClickable(item.bottomSheetActions != null);
    }

    public interface CallBack {
        void clicked(Item item);
    }

    public static class Item {
        private int icon;
        private String data;
        private String localizedDataType;
        private boolean lastInGroup = false;
        @Nullable
        private List<ListChoicePopup.Item> bottomSheetActions;

        public Item(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
            this.icon = icon;
            this.data = data;
            this.localizedDataType = localizedDataType;
            this.bottomSheetActions = bottomSheetActions;
        }

        private Item() {
            this.lastInGroup = true;
        }

        public static Item getDivider() {
            return new Item();
        }

        @Nullable
        public List<ListChoicePopup.Item> getBottomSheetActions() {
            return bottomSheetActions;
        }
    }

    public static class HorizontalItem extends Item {
        public HorizontalItem(int icon, String data, String localizedDataType, @Nullable List<ListChoicePopup.Item> bottomSheetActions) {
            super(icon, data, localizedDataType, bottomSheetActions);
        }
    }

    public class VH extends RecyclerView.ViewHolder {

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
                    cb.clicked(getItem(getAdapterPosition()));
                }
            });
        }
    }
}
