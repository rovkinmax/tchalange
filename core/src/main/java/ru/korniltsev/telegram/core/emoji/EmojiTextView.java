package ru.korniltsev.telegram.core.emoji;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import rx.Subscription;
import rx.functions.Action1;

import javax.inject.Inject;

//invalidates itself when the emojies are loaded
public class EmojiTextView extends TextView{
    @Inject Emoji emoji;
    private Subscription s;

    public EmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        s = emoji.pageLoaded().subscribe(new Action1<Bitmap>() {
            @Override
            public void call(Bitmap bitmap) {
                invalidate();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        s.unsubscribe();
    }
}
