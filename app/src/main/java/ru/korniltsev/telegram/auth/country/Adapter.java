package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.tonicartos.superslim.LayoutManager;
import com.tonicartos.superslim.LinearSLM;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.List;

public class Adapter extends BaseAdapter<Adapter.Item, RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_SECTION = 0;
    public static final int VIEW_TYPE_COUNTRY = 1;
    final CountryClickListener listener;
    interface CountryClickListener {
        void clicked(Countries.Entry c);
    }

    public Adapter(Context ctx, List<Adapter.Item> data, CountryClickListener c) {
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
            View view = getViewFactory()
                    .inflate(R.layout.auth_item_section, parent, false);
            return new SectionVH(view);
        } else {
            View view = getViewFactory()
                    .inflate(R.layout.auth_item_country, parent, false);
            return new VH(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int pos) {
        Adapter.Item item = getItem(pos);

        final LayoutManager.LayoutParams params = (LayoutManager.LayoutParams) vh.itemView.getLayoutParams();
        params.setSlm(LinearSLM.ID);
//        params.setSlm();
        params.setFirstPosition(item.firstItemInSection);
        vh.itemView.setLayoutParams(params);

        if (item instanceof Section) {
            bindSection((SectionVH)vh, (Section) item);
        } else {
            bindCountry((VH)vh, (Country)item);
        }

    }

    private void bindCountry(VH vh, Country item) {
        vh.countryName.setText(item.country.name);
        vh.capitalPhoneCode.setText(item.country.phoneCode);
    }

    private void bindSection(SectionVH vh, Section item) {
        vh.letter.setText(item.letter);
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
                    Country c = (Country) getItem(getPosition());
                    listener.clicked(c.country);
                }
            });
        }
    }

    class SectionVH extends RecyclerView.ViewHolder{
        final TextView letter;

        public SectionVH(View itemView) {
            super(itemView);
            letter = (TextView) itemView.findViewById(R.id.country_letter);
        }
    }

    static abstract class Item {
        final int firstItemInSection;

        public Item(int firstItemInSection) {
            this.firstItemInSection = firstItemInSection;
        }
    }
    static class Section extends Item {
        final String letter;

        Section(String letter, int firstItemInSection) {
            super(firstItemInSection);

            this.letter = letter;
        }
    }

    static class Country extends Item {
        final Countries.Entry country;


        public Country(Countries.Entry country, int firstItemInSection) {
            super(firstItemInSection);
            this.country = country;
        }
    }
}
