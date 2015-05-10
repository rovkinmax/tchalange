/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.korniltsev.telegram.core.app;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import org.telegram.android.DpCalculator;
import org.telegram.android.Emoji;
import ru.korniltsev.telegram.core.audio.AudioPlayer;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChatDB;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.RxGlide;

import javax.inject.Singleton;

/**
 * Defines app-wide singletons.
 */
@Module(
        injects = {
                RXClient.class,
                RXAuthState.class,
                RxGlide.class,
                Emoji.class,
                RxDownloadManager.class,
                AudioPlayer.class,
                DpCalculator.class,
                RxChatDB.class
        },
        library = true)
public class RootModule {
    private Context ctx;

    public RootModule(Context ctx) {
        this.ctx = ctx;
    }

    @Singleton
    @Provides
    Context provideContext(){
        return ctx;
    }


    @Singleton
    @Provides
    Emoji provideEmoji(DpCalculator dpCalculator) {
        return new Emoji(ctx, dpCalculator);
    }

    @Singleton
    @Provides
    DpCalculator provideDpCalc(){
        float density = ctx.getResources().getDisplayMetrics().density;
        return new DpCalculator(density);
    }
}
