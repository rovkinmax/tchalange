package ru.korniltsev.telegram.contacts;

import android.content.Context;
import android.os.Bundle;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat.ChatView;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class ContactsPresenter extends ViewPresenter<ContactListView> implements Action1<TdApi.User> {
    final RXClient client;
    private final Observable<List<Contact>> request;
    private CompositeSubscription subscription;
    private Observable<MeAndChat> requestOpen;

    @Inject
    public ContactsPresenter(RXClient client, final Context appCtx, ChatDB chat) {
        this.client = client;
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
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscription = new CompositeSubscription();
        subscription.add(
                request.subscribe(new ObserverAdapter<List<Contact>>() {
            @Override
            public void onNext(List<Contact> response) {
                getView()
                        .showContacts(response);
            }
        }));
        if (requestOpen != null){
            subscribe();
        }
    }

    @Override
    public void dropView(ContactListView view) {
        super.dropView(view);
        subscription.unsubscribe();
    }

    @Override
    public void call(TdApi.User user) {
        final Observable<TdApi.TLObject> me = client.sendRx(new TdApi.GetMe());
        final Observable<TdApi.TLObject> chat = client.sendRx(new TdApi.CreatePrivateChat(user.id));
        requestOpen = Observable.zip(me, chat, new Func2<TdApi.TLObject, TdApi.TLObject, MeAndChat>() {
            @Override
            public MeAndChat call(TdApi.TLObject tlObject, TdApi.TLObject tlObject2) {
                return new MeAndChat((TdApi.User) tlObject, (TdApi.Chat) tlObject2);
            }
        }).observeOn(mainThread())
                .cache();
        subscribe();
    }

    private void subscribe() {
        subscription.add(
                requestOpen.subscribe(new ObserverAdapter<MeAndChat>() {
            @Override
            public void onNext(MeAndChat response) {
                Flow.get(getView())
                        .set(new Chat(response.tlObject2, response.tlObject));
            }
        }));
    }

    class MeAndChat{

        private final TdApi.User tlObject;
        private final TdApi.Chat tlObject2;

        public MeAndChat(TdApi.User tlObject, TdApi.Chat tlObject2) {

            this.tlObject = tlObject;
            this.tlObject2 = tlObject2;
        }
    }
}
