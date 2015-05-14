package org.telegram.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import ru.korniltsev.telegram.empji.R;

public class EmojiPopup extends PopupWindow implements LayoutObservableLinearLayout.CallBack {
    final LayoutObservableLinearLayout rootView;
    private final WindowManager wm;
    private boolean keyboardVisible;
    private DpCalculator calc;
    private final Context ctx;
    private final SharedPreferences prefs;

    public EmojiPopup(View contentView, LayoutObservableLinearLayout rootView, DpCalculator calc) {
        super(contentView);
        this.rootView = rootView;
        this.calc = calc;
        ctx = contentView.getContext();
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
        rootView.setCallback(null);
        rootView.setPadding(0,0,0,0);
    }


    public static EmojiPopup create(Activity ctx, LayoutObservableLinearLayout parent, DpCalculator calc) {
        LayoutInflater viewFactory = LayoutInflater.from(ctx);
        EmojiKeyboardView view = (EmojiKeyboardView)  viewFactory.inflate(R.layout.view_emoji_keyboard, null, false);
        EmojiPopup res = new EmojiPopup(view, parent, calc);



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
            rootView.setPadding(0, 0, 0, 0);
            saveKeyboardHiehgt(keyboardHeight);
            View contentView = getContentView();
            final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) contentView.getLayoutParams();
            if (layoutParams.height != keyboardHeight) {
                wm.updateViewLayout(contentView, layoutParams);
            }
        }
    }
}
