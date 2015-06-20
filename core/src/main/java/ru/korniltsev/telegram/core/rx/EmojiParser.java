package ru.korniltsev.telegram.core.rx;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.Emoji;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;

@Singleton
public class EmojiParser {
    final Emoji emoji;
    //guarded by client thread
    final Map<String, CharSequence> cache = new HashMap<>();
    //    stolen here
    //    https://github.com/regexps/mentions-regex/blob/master/index.js
    private final Pattern userReference = Pattern.compile("(?:^|[^a-zA-Z0-9_＠!@#$%&*])(?:(?:@|＠)(?!/))([a-zA-Z0-9/_]{1,15})(?:\\b(?!@|＠)|$)");

    @Inject
    public EmojiParser(Emoji emoji) {
        this.emoji = emoji;
    }

    public void parse(TdApi.Message msg) {
        //        checkNotMainThread();
        if (msg.message instanceof TdApi.MessageText) {
            parseEmojis(msg);
        } else if (msg.message instanceof TdApi.MessagePhoto) {
            pareImageSizes((TdApi.MessagePhoto) msg.message);
        }
    }

    private void pareImageSizes(TdApi.MessagePhoto message) {
        //todo rename the class
        for (TdApi.PhotoSize photo : message.photo.photos) {
            if (photo.photo instanceof TdApi.FileLocal){
                if (photo.width == 0 || photo.height == 0) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(((TdApi.FileLocal) photo.photo).path, o);
                    photo.width = o.outWidth;
                    photo.height= o.outHeight;

                }

            }

        }
    }

    private void parseEmojis(TdApi.Message msg) {
        TdApi.MessageText text = (TdApi.MessageText) msg.message;
        String key = text.text;
        CharSequence fromCache = cache.get(key);
        if (fromCache != null) {
            text.textWithSmilesAndUserRefs = fromCache;
        } else {
            CharSequence parsed = emoji.replaceEmoji(key);
            Matcher matcher = userReference.matcher(key);
            Spannable s;
            if (parsed instanceof Spannable) {
                s = (Spannable) parsed;
            } else {
                s = Spannable.Factory.getInstance().newSpannable(parsed);
            }

            while (matcher.find()) {
                s.setSpan(new ForegroundColorSpan(0xff427ab0), matcher.start(), matcher.end(), 0);
            }
            cache.put(key, s);
            text.textWithSmilesAndUserRefs = s;
        }
    }
}
