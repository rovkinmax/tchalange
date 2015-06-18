package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.R;

import javax.inject.Inject;
import java.util.List;

import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class SelectCountryView extends LinearLayout {
    @Inject SelectCountry.Presenter presenter;
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
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Countries.Entry> countries = new Countries()
                .getData(getContext());
        list.setAdapter(new Adapter(getContext(), countries, new Adapter.CountryClickListener() {
            @Override
            public void clicked(Countries.Entry c) {
                presenter.countrySelected(c);
            }
        }));
    }

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
