package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;

public interface UserHolder {
    boolean hasUserWith(int id);
    TdApi.User getUser(int id);
    void saveUser(TdApi.User u);
}
