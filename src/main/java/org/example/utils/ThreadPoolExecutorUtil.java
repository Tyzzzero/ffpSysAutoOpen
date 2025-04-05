package org.example.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 */
public class ThreadPoolExecutorUtil {
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

    public static ThreadPoolExecutor createThreadPool(int threadCount) {
        return new ThreadPoolExecutor(
            threadCount,
            threadCount,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("PlaywrightThread-" + THREAD_NUMBER.getAndIncrement());
                return thread;
            }
        );
    }
} 