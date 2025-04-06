package org.example.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 * @author Tyzzzero
 */
public class ThreadPoolExecutorUtil {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolExecutorUtil.class);
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
    private static final int QUEUE_CAPACITY = 1000;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final RejectedExecutionHandler REJECTED_EXECUTION_HANDLER = (r, executor) -> {
        log.warn("任务被拒绝执行: {}", r);
        if (!executor.isShutdown()) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                log.error("任务重新入队失败: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    };

    public static ThreadPoolExecutor createThreadPool(int threadCount) {
        log.info("创建线程池，线程数: {}", threadCount);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threadCount,
            threadCount,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("PlaywrightThread-" + THREAD_NUMBER.getAndIncrement());
                thread.setUncaughtExceptionHandler((t, e) -> 
                    log.error("线程[{}]发生未捕获异常: {}", t.getName(), e.getMessage(), e));
                return thread;
            },
            REJECTED_EXECUTION_HANDLER
        );
        
        // 添加线程池监控
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("ThreadPoolMonitor");
            thread.setDaemon(true); // 设置为守护线程
            return thread;
        });
        
        ScheduledFuture<?> monitorFuture = monitor.scheduleAtFixedRate(() -> {
            if (executor.isShutdown()) {
                monitor.shutdown();
                return;
            }
            log.debug("线程池状态: 核心线程数={}, 活动线程数={}, 最大线程数={}, 队列大小={}, 已完成任务数={}",
                executor.getCorePoolSize(),
                executor.getActiveCount(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());
        }, 0, 30, TimeUnit.SECONDS);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                monitorFuture.cancel(true);
                monitor.shutdown();
                if (!monitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("监控线程关闭失败: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }));
        
        return executor;
    }
} 