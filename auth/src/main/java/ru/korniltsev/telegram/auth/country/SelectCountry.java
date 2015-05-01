package ru.korniltsev.telegram.auth.country;

import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.auth.R;
import ru.korniltsev.telegram.auth.phone.EnterPhoneFragment;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.flow.utils.Utils;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

/**
 * Created by korniltsev on 22/04/15.
 */
@WithModule(SelectCountry.Module.class)
public class SelectCountry extends BasePath implements Serializable {
    @Override
    public int getRootLayout() {
        return R.layout.fragent_select_country;
    }
    @dagger.Module(injects = SelectCountryView.class, addsTo = RootModule.class)
    public static class Module {

    }
    @Singleton
    static class Presenter extends ViewPresenter<SelectCountryView> {
        @Inject
        public Presenter() {
        }

        public void countrySelected(Countries.Entry c) {
            EnterPhoneFragment prev = Utils.getPreviousPath(getView());
            prev.setCountry(c);
            Flow.get(getView())
                    .goBack();

        }
    }
    //    Toolbar toolbar;
//    RecyclerView list;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragent_select_country, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        initToolbar(view)
//                .setTitle(R.string.country)
//                .pop();
//        list = (RecyclerView) view.findViewById(R.id.list);
//        list.setLayoutManager(new LinearLayoutManager(getActivity()));
//        List<Countries.Entry> countries = new Countries(getActivity())
//                .getData();
//        list.setAdapter(new Adapter(getActivity(), countries, new Adapter.CountryClickListener() {
//            @Override
//            public void clicked(Countries.Entry c) {
//                FlowLike.from(getActivity())
//                        .pop(c);
//            }
//        }));
//    }
}
