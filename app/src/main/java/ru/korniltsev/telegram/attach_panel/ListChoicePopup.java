package ru.korniltsev.telegram.attach_panel;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import junit.framework.Assert;
import ru.korniltsev.telegram.chat.R;

import java.util.List;

public class ListChoicePopup extends AttachPanelPopup {

//    private LayoutInflater from;

    public static ListChoicePopup create(Activity ctx, List<Item> data) {
        ListChoicePopup res = new ListChoicePopup(ctx, data);
        res.show(ctx);
        return res;
    }

    final List<Item> data;

    ListChoicePopup(Context ctx, List<Item> data) {
        super(ctx);
        this.data = data;
        Assert.assertNotNull(data);
        initView();

        //        from = LayoutInflater.from(ctx);
    }

    @Override
    protected void inflatePanel(ViewGroup view) {
        LayoutInflater.from(view.getContext())
                .inflate(R.layout.attach_panel_panel_empty, view, true);
    }

    @Override
    protected void initView() {
        final LayoutInflater viewFactory = LayoutInflater.from(getContentView().getContext());
        for (final Item item : data) {
            final TextView itemView = (TextView) viewFactory.inflate(R.layout.attach_panel_panel_item, panel, false);
            itemView.setText(item.localizedText);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.click.run();
                    dismiss();
                }
            });
            panel.addView(itemView);
        }
    }


    public static final class Item {
        final String localizedText;
        final Runnable click;

        public Item(String localizedText, Runnable click) {
            this.localizedText = localizedText;
            this.click = click;
        }
    }
}
