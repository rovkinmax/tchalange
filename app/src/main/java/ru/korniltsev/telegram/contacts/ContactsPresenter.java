package ru.korniltsev.telegram.contacts;

import android.content.Context;
import android.os.Bundle;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.ChatView;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class ContactsPresenter extends ViewPresenter<ContactListView> {
    final RXClient client;
    private final Observable<List<Contact>> request;
    private Subscription subscription;
    private final Context appCtx;
    @Inject
    public ContactsPresenter(RXClient client, final Context appCtx) {
        this.client = client;
        this.appCtx = appCtx;
        request = client.sendCachedRXUI(new TdApi.GetContacts())
                .map(new Func1<TdApi.TLObject, List<Contact>>() {
                    @Override
                    public List<Contact> call(TdApi.TLObject response) {
                        TdApi.Contacts contacts = (TdApi.Contacts) response;
                        final ArrayList<Contact> res = new ArrayList<>();
                        for (TdApi.User user : contacts.users) {
                            final String uiStatus = ChatView.uiUserStatus(appCtx, user.status);
                            res.add(new Contact(user, AppUtils.uiName(user, appCtx), uiStatus));
                        }
                        Collections.sort(res, new Comparator<Contact>() {
                            @Override
                            public int compare(Contact lhs, Contact rhs) {
                                return lhs.uiName.compareTo(rhs.uiName);
                            }
                        });
                        return res;
                    }
                })
                .observeOn(mainThread())
                .cache();
//                .compose(new RXClient.FilterAndCastToClass<>(TdApi.Contacts.class))
//        .map(new Func1<TdApi.Contacts, List<Contact>>() {
//            @Override
//            public List<Contact> call(TdApi.Contacts contacts) {
//
//                return res;
//            }
//        });
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscription = request.subscribe(new ObserverAdapter<List<Contact>>() {
            @Override
            public void onNext(List<Contact> response) {
                getView()
                        .showContacts(response);
            }
        });

    }

    @Override
    public void dropView(ContactListView view) {
        super.dropView(view);
        subscription.unsubscribe();
    }
}
