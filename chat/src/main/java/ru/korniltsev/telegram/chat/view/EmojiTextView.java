package ru.korniltsev.telegram.chat.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.telegram.android.Emoji;

import javax.inject.Inject;

public class EmojiTextView extends TextView{
    @Inject Emoji emoji;
    public EmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (emoji == null){ //setText may be called before injection
            return;
        }
        super.setText(
                emoji.replaceEmoji(text),
                type);
    }
}
