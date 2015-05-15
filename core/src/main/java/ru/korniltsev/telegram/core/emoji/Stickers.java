package ru.korniltsev.telegram.core.emoji;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

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
    public Stickers(RXClient client) {
        this.client = client;
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
        Collections.addAll(ss, newStickers.stickers);
    }

    public List<TdApi.Sticker> getStickers() {
        return ss;
    }
}
