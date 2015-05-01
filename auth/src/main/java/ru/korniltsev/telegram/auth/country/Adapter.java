package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;

import java.util.List;

public class Adapter extends BaseAdapter<Countries.Entry, Adapter.VH> {
    final CountryClickListener listener;
    interface CountryClickListener {
        void clicked(Countries.Entry c);
    }

    public Adapter(Context ctx, List<Countries.Entry> data, CountryClickListener c) {
        super(ctx, data);
        this.listener = c;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int i) {
        View view = getViewFactory()
                .inflate(R.layout.item_country, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(VH vh, int pos) {
        Countries.Entry item = getItem(pos);
        if (showCapitalLetter(pos)){
            vh.letter.setVisibility(View.VISIBLE);
        } else {
            vh.letter.setVisibility(View.INVISIBLE);
        }
        vh.letter.setText(item.firstLetter);
        vh.countryName.setText(item.name);
        vh.capitalPhoneCode.setText(item.phoneCode);
    }

    private boolean showCapitalLetter(int pos) {
        if (pos == 0) return true;
        Countries.Entry prev = getItem(pos-1);
        Countries.Entry current = getItem(pos);
        return prev.name.charAt(0) != current.name.charAt(0);
    }


    class VH extends RecyclerView.ViewHolder{
        final TextView letter;
        final TextView countryName;
        final TextView capitalPhoneCode;

        public VH(View itemView) {
            super(itemView);
            capitalPhoneCode = (TextView) itemView.findViewById(R.id.country_phone_code);
            letter = (TextView) itemView.findViewById(R.id.country_letter);
            countryName = (TextView) itemView.findViewById(R.id.country_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Countries.Entry c = getItem(getPosition());
                    listener.clicked(c);
                }
            });
        }
    }
}
