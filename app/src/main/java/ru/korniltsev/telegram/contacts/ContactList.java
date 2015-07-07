package ru.korniltsev.telegram.contacts;

import flow.path.Path;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.ForwardedMessageView;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(ContactList.Module.class)
public class ContactList extends BasePath implements Serializable {

    @Override
    public int getRootLayout() {
        return R.layout.contacts_view;
    }

    @dagger.Module(
            injects = {
                    ContactListView.class,
            },
            addsTo = RootModule.class)
    public static class Module {

    }
}
