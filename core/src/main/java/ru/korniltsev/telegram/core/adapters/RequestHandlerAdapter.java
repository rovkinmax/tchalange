package ru.korniltsev.telegram.core.adapters;

import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TdApi;

public class RequestHandlerAdapter implements Client.ResultHandler {
    public static RequestHandlerAdapter INSTANCE = new RequestHandlerAdapter();
    @Override
    public final void onResult(TdApi.TLObject object) {

    }
}
