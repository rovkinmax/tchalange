package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.recycler.sections.Item;
import ru.korniltsev.telegram.common.recycler.sections.Section;
import ru.korniltsev.telegram.common.recycler.sections.SectionVH;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.List;

public class Adapter extends BaseAdapter<Item<Countries.Entry>, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_SECTION = 0;
    public static final int VIEW_TYPE_COUNTRY = 1;
    final CountryClickListener listener;
    interface CountryClickListener {
        void clicked(Countries.Entry c);
    }

    public Adapter(Context ctx, List<Item<Countries.Entry>> data, CountryClickListener c) {
        super(ctx, data);
        this.listener = c;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof Section) {
            return VIEW_TYPE_SECTION;
        } else {
            return VIEW_TYPE_COUNTRY;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION){
            return new SectionVH(getViewFactory().inflate(R.layout.auth_item_section, parent, false));
        } else {
            View view = getViewFactory()
                    .inflate(R.layout.auth_item_country, parent, false);
            return new VH(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int pos) {
        Item<Countries.Entry> item = getItem(pos);

        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) vh.itemView.getLayoutParams();
        params.setSlm(LinearSLM.ID);
        params.setFirstPosition(item.firstPosition);
        vh.itemView.setLayoutParams(params);

        if (item instanceof Section) {
            final SectionVH vh1 = (SectionVH) vh;
            vh1.bind((Section)item);

        } else {
            bindCountry((VH)vh, item.data);
        }

    }

    private void bindCountry(VH vh, Countries.Entry item) {
        vh.countryName.setText(item.localizedName());
        vh.capitalPhoneCode.setText(item.phoneCode);
    }






    class VH extends RecyclerView.ViewHolder{
        final TextView countryName;
        final TextView capitalPhoneCode;

        public VH(View itemView) {
            super(itemView);
            capitalPhoneCode = (TextView) itemView.findViewById(R.id.country_phone_code);
            countryName = (TextView) itemView.findViewById(R.id.country_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Item<Countries.Entry> item = getItem(getPosition());
                    listener.clicked(item.data);
                }
            });
        }
    }

}
