package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.tonicartos.superslim.LayoutManager;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.recycler.sections.Item;
import ru.korniltsev.telegram.common.recycler.sections.Section;

import javax.inject.Inject;
import java.util.List;

import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class SelectCountryView extends LinearLayout {
    @Inject SelectCountry.Presenter presenter;
    @Inject Countries countries;
    private RecyclerView list;

    public SelectCountryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initToolbar(this)
                .setTitle(R.string.country)
                .pop();
        list = (RecyclerView) findViewById(R.id.list);

        final LayoutManager lm = new LayoutManager(getContext());
        list.setLayoutManager(lm);
        List<Countries.Entry> countries = this.countries
                .getData();
        final List<Item<Countries.Entry>> sectionedData = Section.prepareListOf(countries, new Section.SectionFactory<Countries.Entry>() {
            @Override
            public String sectionForItem(Countries.Entry entry) {
                return entry.firstLetter;
            }

            @Override
            public long id(Countries.Entry entry) {
                return entry.position;
            }
        });
        list.setAdapter(new Adapter(getContext(), sectionedData, new Adapter.CountryClickListener() {
            @Override
            public void clicked(Countries.Entry c) {
                presenter.countrySelected(c);
            }
        }));
    }

//    private List<Adapter.Item> prepareListOf(List<Countries.Entry> countries) {
//        final ArrayList<Adapter.Item> res = new ArrayList<>();
//        Countries.Entry previous = null;
//        int lastSectionPos = -1;
//        for (Countries.Entry it : countries) {
//            if (previous == null ||
//                    !previous.firstLetter.equals(it.firstLetter)) {
//                lastSectionPos = res.size();
//                res.add(new Adapter.Section(it.firstLetter, lastSectionPos));
//            }
//            res.add(new Adapter.Country(it, lastSectionPos));
//            previous = it;
//        }
//
//        return res;
//    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }
}
