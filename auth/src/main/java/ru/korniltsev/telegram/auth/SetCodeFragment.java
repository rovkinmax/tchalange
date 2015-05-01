//package ru.korniltsev.telegram.auth;
//
//import android.os.Bundle;
//import android.support.v7.widget.Toolbar;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import ru.korniltsev.telegram.core.app.MyApp;
//import ru.korniltsev.telegram.core.flow.BaseFragment;
//import ru.korniltsev.telegram.core.rx.ObserverAdapter;
//import ru.korniltsev.telegram.core.rx.RXAuthState;
//import ru.korniltsev.telegram.core.rx.RXClient;
//import org.drinkless.td.libcore.telegram.TdApi;
//
//import static ru.korniltsev.telegram.core.Utils.textFrom;
//import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;
//import static junit.framework.Assert.fail;
//
///**
// * Created by korniltsev on 23/04/15.
// */
//public class SetCodeFragment extends BaseFragment {
//
//    private EditText smsCode;
//    private Toolbar toolbar;
//    private RXAuthState rxAuthState;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_set_code, container, false);
//    }
//
//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        initToolbar(view)
//                .setTitle(R.string.activation_code)
//                .pop()
//                .addMenuItem(R.menu.set_code, R.id.menu_set_code, new Runnable() {
//                    @Override
//                    public void run() {
//                        checkCode();
//                    }
//                });
//
//        smsCode = ((EditText) view.findViewById(R.id.sms_code));
//    }
//
//    private void checkCode() {
//        TdApi.AuthSetCode f = new TdApi.AuthSetCode(
//                textFrom(smsCode)
//        );
//        final RXClient rxClient = getRxClient();
//        rxAuthState = MyApp.from(this)
//                .getRxAuthState();
//        rxClient.sendRXUI(f)
//                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
//                    @Override
//                    public void onNext(TdApi.TLObject response) {
//                        if (response instanceof TdApi.AuthStateOk) {
//                            rxAuthState.authorized();
//                        } else {
//                            fail("unimplemented");
//                        }
//                    }
//                });
//    }
//
//
//}
