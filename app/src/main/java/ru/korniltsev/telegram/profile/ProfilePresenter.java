package ru.korniltsev.telegram.profile;

import android.os.Bundle;
import mortar.ViewPresenter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProfilePresenter extends ViewPresenter<ProfileView> {
    final ProfilePath path;

    @Inject
    public ProfilePresenter(ProfilePath path) {
        this.path = path;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
//        if (path.user != null) {
            getView().bindUser(path.user);
//        } else if (path.groupChat != null){
//            getView().bindChat(path.groupChat);
//        }
    }
}
