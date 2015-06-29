package ru.korniltsev.telegram.core.app;

import android.os.Process;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class AndroidBackgroundPriorityThreadFactory implements ThreadFactory {

    private final String threadName ;
    private final AtomicLong counter = new AtomicLong();

    public AndroidBackgroundPriorityThreadFactory(String threadPrefix) {
        this.threadName = threadPrefix;
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                r.run();
            }
        });
        thread.setName(threadName + " #" + counter.incrementAndGet());
        return thread;
    }
}
