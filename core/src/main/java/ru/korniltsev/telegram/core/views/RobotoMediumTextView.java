package ru.korniltsev.telegram.core.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

public class RobotoMediumTextView extends TextView {
    public static final String FONT = "fonts/Roboto-Medium.ttf";
    public static Typeface loadedFont;
    public RobotoMediumTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            if (loadedFont == null) {
                AssetManager assets = getContext().getAssets();
                loadedFont = Typeface.createFromAsset(assets, FONT);
            }
            setTypeface(loadedFont, 0);
        } else {
            setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
    }


}
