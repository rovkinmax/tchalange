package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import org.telegram.android.Emoji;
import ru.korniltsev.telegram.core.utils.Preconditions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;

@Singleton
public class EmojiParser {
    final Emoji emoji;
    //guarded by client thread
    final Map<String, CharSequence> cache = new HashMap<>();
    @Inject
    public EmojiParser(Emoji emoji) {
        this.emoji = emoji;
    }

    public void parse(TdApi.Message msg) {
        checkNotMainThread();
        if (msg.message instanceof TdApi.MessageText){
            TdApi.MessageText text = (TdApi.MessageText) msg.message;
            String key = text.text;
            CharSequence fromCache = cache.get(key);
            if (fromCache != null){
                text.textWithSmilesAndUserRefs = fromCache;
            } else {
                CharSequence parsed = emoji.replaceEmoji(key);
                text.textWithSmilesAndUserRefs = parsed;
                cache.put(key, parsed);
            }
        }
    }
}
