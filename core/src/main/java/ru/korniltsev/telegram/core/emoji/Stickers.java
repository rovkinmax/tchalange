package ru.korniltsev.telegram.core.emoji;

import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class Stickers {
    final RXClient client;
    private List<TdApi.Sticker> ss = new ArrayList<>();



    @Inject
    public Stickers(final RXClient client, final RXAuthState auth) {
        this.client = client;

        handleState(auth.getState());
        auth.listen()
                .subscribe(new Action1<RXAuthState.AuthState>() {
                    @Override
                    public void call(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateAuthorized){
//                            client.sendSilently(new TdApi.GetContacts());
                        } else {
                            ss.clear();
                        }

                    }
                });

        client.stickerUpdates()
                .subscribe(new Action1<TdApi.UpdateStickers>() {
                    @Override
                    public void call(TdApi.UpdateStickers updateStickers) {
                        Log.e("FindStickerBug", "got update sticker");
                        requestStickers();
                    }
                });
    }

    private void handleState( RXAuthState.AuthState auth) {
        if (auth instanceof RXAuthState.StateAuthorized){
            requestStickers();
        }
    }

    private void requestStickers() {
        client.sendRx(new TdApi.GetStickers(""))
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.TLObject>() {
                    @Override
                    public void call(TdApi.TLObject tlObject) {

                        updateStickers((TdApi.Stickers) tlObject);
                    }
                });
    }

    private void updateStickers(TdApi.Stickers newStickers) {
        ss.clear();
        Log.e("FindStickerBug", "got stickers" + newStickers.stickers.length);
        Collections.addAll(ss, newStickers.stickers);
    }

    public List<TdApi.Sticker> getStickers() {
        return ss;
    }
}
