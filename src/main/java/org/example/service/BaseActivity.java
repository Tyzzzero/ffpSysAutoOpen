package org.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.*;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.controller.LoginHandler;
import org.example.controller.MenuHandler;
import org.example.controller.ModuleHandler;
import org.example.entity.MenuEnum;
import org.example.entity.Model;
import org.example.entity.ModuleEnum;
import org.example.utils.ThreadPoolExecutorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tyzzzero
 * 抽象类 BaseActivity
 */
public abstract class BaseActivity {
    private static final Config config = ConfigLoader.loadConfig(Config.class);
    private static final Logger log = LoggerFactory.getLogger(BaseActivity.class);
    protected static List<Model> modelList;
    protected static final List<Model> failureModelList = new CopyOnWriteArrayList<>();
    protected static final List<Model> successModelList = new CopyOnWriteArrayList<>();
    private final ModuleHandler moduleHandler = new ModuleHandler();
    private final MenuHandler menuHandler = new MenuHandler();
    private final LoginHandler loginHandler = new LoginHandler();
    private static final ThreadLocal<Page> threadLocalPage = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, Playwright> playwrightMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Browser> browserMap = new ConcurrentHashMap<>();
    private static final AtomicInteger processedCount = new AtomicInteger(0);

    // 执行方法
    public void execute(String inputFileName, ModuleEnum module, MenuEnum menu) {
        TimeInterval timeInterval = new TimeInterval();
        timeInterval.start("overall");
        try {
            onCreate(inputFileName);
            log.info("开始处理，总任务数：{}，线程数：{}，模块：{}，菜单：{}", 
                modelList.size(), config.getThreadCount(), module, menu);
            
            ThreadPoolExecutor executor = ThreadPoolExecutorUtil.createThreadPool(config.getThreadCount());
            int totalSize = modelList.size();
            int batchSize = (int) Math.ceil((double) totalSize / config.getThreadCount());
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < totalSize; i += batchSize) {
                int start = i;
                int end = Math.min(start + batchSize, totalSize);
                List<Model> subList = modelList.subList(start, end);
                futures.add(executor.submit(() -> {
                    String threadName = Thread.currentThread().getName();
                    try {
                        initPlaywright(threadName);
                        loginHandler.login(threadLocalPage.get());
                        moduleHandler.openModule(threadLocalPage.get(), module);
                        menuHandler.openMenu(threadLocalPage.get(), menu);
                        
                        for (Model model : subList) {
                            processSingleModel(model, threadName, module, menu);
                        }
                    } catch (Exception e) {
                        log.error("[{}] 线程执行异常: {}", threadName, e.getMessage(), e);
                    } finally {
                        cleanupPlaywright(threadName);
                    }
                }));
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    log.error("任务被中断: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    log.error("任务执行异常: {}", e.getMessage(), e);
                }
            }
            
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                log.warn("线程池未能在指定时间内关闭，强制关闭");
                executor.shutdownNow();
            }
            
            onDestroy();
            long totalTime = timeInterval.intervalMs("overall");
            log.info("处理完成，总耗时：{}，成功：{}，失败：{}，总处理：{}，总读取：{}，成功率：{}%",
                formatDuration(totalTime),
                successModelList.size(),
                failureModelList.size(),
                processedCount.get(),
                modelList.size(),
                (double) successModelList.size() / modelList.size() * 100);
        } catch (Exception e) {
            log.error("系统执行异常: {}", e.getMessage(), e);
        }
    }

    private void processSingleModel(Model model, String threadName, ModuleEnum module, MenuEnum menu) {
        TimeInterval singleInterval = new TimeInterval();
        singleInterval.start("single");
        log.info("[{}] 开始处理记录: model={}", threadName, JSON.toJSONString(model));
        
        for (int retryCount = 0; retryCount <= config.getRetryCount(); retryCount++) {
            try {
                onStart(threadLocalPage.get(), model);
                addToSuccessList(model);
                long processTime = singleInterval.intervalMs("single");
                log.info("[{}] 处理成功: model={}, 耗时: {}, 重试次数: {}",
                    threadName, JSON.toJSONString(model), formatDuration(processTime), retryCount);
                break;
            } catch (Exception e) {
                log.warn("[{}] 处理失败: model={}, 重试次数: {}, 错误: {}",
                    threadName, JSON.toJSONString(model), retryCount, e.getMessage());
                
                if (retryCount == config.getRetryCount()) {
                    addToFailureList(model);
                    log.error("[{}] 最终处理失败: model={}, 错误: {}",
                        threadName, JSON.toJSONString(model), e.getMessage());
                } else {
                    try {
                        retryOperation(threadName, module, menu);
                    } catch (Exception retryEx) {
                        log.error("[{}] 重试操作失败: {}", threadName, retryEx.getMessage(), retryEx);
                    }
                }
            }
        }
        processedCount.incrementAndGet();
        long totalTime = singleInterval.intervalMs("single");
        log.info("[{}] 处理完成: model={}, 总耗时: {}",
            threadName, JSON.toJSONString(model), formatDuration(totalTime));
    }

    private void retryOperation(String threadName, ModuleEnum module, MenuEnum menu) {
        log.info("[{}] 开始重试操作", threadName);
        threadLocalPage.get().reload();
        moduleHandler.openModule(threadLocalPage.get(), module);
        menuHandler.openMenu(threadLocalPage.get(), menu);
        log.info("[{}] 重试操作完成", threadName);
    }

    private void initPlaywright(String threadName) {
        try {
            Playwright playwright = Playwright.create();
            playwrightMap.put(threadName, playwright);
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            browserMap.put(threadName, browser);
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.setDefaultTimeout(config.getGlobalTimeout());
            threadLocalPage.set(page);
            page.navigate(config.getUrl());
            log.info("[{}] Playwright初始化成功", threadName);
        } catch (Exception e) {
            log.error("[{}] Playwright初始化失败: {}", threadName, e.getMessage(), e);
            throw e;
        }
    }

    private void cleanupPlaywright(String threadName) {
        try {
            Page page = threadLocalPage.get();
            if (page != null) {
                page.close();
                threadLocalPage.remove();
            }
            Browser browser = browserMap.remove(threadName);
            if (browser != null) {
                browser.close();
            }
            Playwright playwright = playwrightMap.remove(threadName);
            if (playwright != null) {
                playwright.close();
            }
            log.info("[{}] Playwright资源清理完成", threadName);
        } catch (Exception e) {
            log.error("[{}] Playwright资源清理失败: {}", threadName, e.getMessage(), e);
        }
    }

    protected void addToSuccessList(Model model) {
        successModelList.add(model);
    }

    protected void addToFailureList(Model model) {
        failureModelList.add(model);
    }

    protected void onCreate(String inputFileName) throws Exception {
        try {
            modelList = new ArrayList<>();
            failureModelList.clear();
            successModelList.clear();
            processedCount.set(0);
            
            InputStream inputStream = BaseActivity.class.getClassLoader().getResourceAsStream(inputFileName);
            if (inputStream == null) {
                throw new FileNotFoundException("找不到输入文件: " + inputFileName);
            }
            
            EasyExcel.read(inputStream, Model.class, new PageReadListener<Model>(dataList -> {
                modelList.addAll(dataList);
                log.debug("读取到{}条数据", dataList.size());
            })).sheet().doRead();
            
            log.info("数据读取完成，总条数：{}", modelList.size());
        } catch (Exception e) {
            log.error("数据读取失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    protected abstract void onStart(Page page, Model model);

    protected void onDestroy() throws Exception {
        try {
            String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String fileName = "target/output/" + date + "_result.xlsx";
            File directory = new File("target/output");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("无法创建输出目录: " + directory.getAbsolutePath());
            }
            
            failureModelList.sort(Comparator.comparing(Model::getId));
            successModelList.sort(Comparator.comparing(Model::getId));
            
            try (ExcelWriter excelWriter = EasyExcel.write(fileName, Model.class).build()) {
                WriteSheet failureSheet = EasyExcel.writerSheet(0, "失败数据").build();
                excelWriter.write(failureModelList, failureSheet);
                
                WriteSheet successSheet = EasyExcel.writerSheet(1, "成功数据").build();
                excelWriter.write(successModelList, successSheet);
                
                WriteSheet allSheet = EasyExcel.writerSheet(2, "全部数据").build();
                excelWriter.write(modelList, allSheet);
            }
            
            log.info("结果文件已生成: {}", fileName);
        } catch (Exception e) {
            log.error("结果文件生成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 格式化耗时
     * @param milliseconds 毫秒数
     * @return 格式化的耗时字符串
     */
    private String formatDuration(long milliseconds) {
        long hours = milliseconds / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (milliseconds % (1000 * 60)) / 1000;
        long ms = milliseconds % 1000;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("分钟");
        }
        if (seconds > 0 || minutes > 0 || hours > 0) {
            sb.append(seconds).append("秒");
        }
        sb.append(ms).append("毫秒");
        
        return sb.toString();
    }
}