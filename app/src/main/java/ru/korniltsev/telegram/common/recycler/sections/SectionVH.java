package ru.korniltsev.telegram.common.recycler.sections;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;

public class SectionVH extends RecyclerView.ViewHolder{
    final TextView letter;

    public SectionVH(View view) {
        super(view);
        letter = (TextView) itemView.findViewById(R.id.country_letter);
    }



    public  <T> void bind(Section<T> t){
        letter.setText(t.firstChar);
    }
}
