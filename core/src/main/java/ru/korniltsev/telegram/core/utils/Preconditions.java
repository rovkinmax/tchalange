package ru.korniltsev.telegram.core.utils;

import android.os.Handler;
import android.os.Looper;
import junit.framework.Assert;

import static junit.framework.Assert.assertTrue;

public class Preconditions {
    public static final Looper MAIN_LOOPER = Looper.getMainLooper();
    public static final Thread MAIN_THREAD = MAIN_LOOPER.getThread();
    public static final Handler MAIN_HANDLER = new Handler(MAIN_LOOPER);

    public static void checkMainThread(){
        assertTrue(MAIN_THREAD == Thread.currentThread());
    }
    public static void checkNotMainThread(){
        assertTrue(MAIN_THREAD != Thread.currentThread());
    }
}
