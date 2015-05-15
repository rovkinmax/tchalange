package ru.korniltsev.telegram.core.emoji;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.utils.R;

import javax.inject.Inject;

public class EmojiPopup extends PopupWindow implements ObservableLinearLayout.CallBack {
    final ObservableLinearLayout parentView;
    private final WindowManager wm;
    private boolean keyboardVisible;
    @Inject  DpCalculator calc;
    private final Context ctx;
    private final SharedPreferences prefs;
    EmojiKeyboardView view;



    public EmojiPopup(EmojiKeyboardView view, ObservableLinearLayout rootView) {
        super(view);
        this.view = view;
        this.parentView = rootView;
        ObjectGraphService.inject(view.getContext(), this);
        ctx = view.getContext();
        prefs = ctx.getSharedPreferences("EmojiPopup", Context.MODE_PRIVATE);
        rootView.setCallback(this);

        int keyboardHeight = rootView.getKeyboardHeight();
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();//todo landscape
        int height;
        if (keyboardHeight > 0) {
            height = keyboardHeight;
            saveKeyboardHiehgt(keyboardHeight);
        } else {
            height = guessKeyboardHeight();
        }


        setWidth(exactly(width));
        setHeight(exactly(height));

        keyboardVisible = keyboardHeight > 0;
        if (!keyboardVisible) {
            rootView.setPadding(0, 0, 0, height);
        }
    }

    private void saveKeyboardHiehgt(int keyboardHeight) {
        boolean portrait = isPortrait();
        prefs.edit()
                .putInt(getKeyForConfiguration(portrait), keyboardHeight)
                .commit();
    }

    private int guessKeyboardHeight() {
        boolean portrait = isPortrait();
        String prefKey = getKeyForConfiguration(portrait);
        return prefs.getInt(prefKey, calc.dp(portrait ? 240 : 150));
    }

    private String getKeyForConfiguration(boolean portrait){
        String prefKey;
        prefKey = "keyboard_height_" + portrait;
        return prefKey;
    }
    private boolean isPortrait(){
        int orientation = ctx.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        return portrait;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        parentView.setCallback(null);
        parentView.setPadding(0, 0, 0, 0);
    }


    public static EmojiPopup create(Activity ctx, ObservableLinearLayout parent, EmojiKeyboardView.CallBack cb) {
        LayoutInflater viewFactory = LayoutInflater.from(ctx);
        EmojiKeyboardView view = (EmojiKeyboardView)  viewFactory.inflate(R.layout.view_emoji_keyboard, null, false);
        view.setCallback(cb);
//        view.setEmoji(emoji);

        EmojiPopup res = new EmojiPopup(view, parent);



        res.showAtLocation(ctx.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.LEFT, 0, 0);

        return res;
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
    }

    @Override
    public void onLayout(int keyboardHeight, boolean landscape) {
        boolean newKeyboardVisible = keyboardHeight > 0;
        if (keyboardVisible == newKeyboardVisible){
            return;
        }
        //keyboard shown or hidden
        keyboardVisible = newKeyboardVisible;
        if (!keyboardVisible) {
            //if hidden - dissmiss
            dismiss();
        } else {
            //if shown - update layout
            parentView.setPadding(0, 0, 0, 0);
            saveKeyboardHiehgt(keyboardHeight);
            View contentView = getContentView();
            final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) contentView.getLayoutParams();
            if (layoutParams.height != keyboardHeight) {
                wm.updateViewLayout(contentView, layoutParams);
            }
        }
    }
}
