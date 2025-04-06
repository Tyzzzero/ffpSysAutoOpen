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
        try {
            TimeInterval timeInterval = new TimeInterval();
            timeInterval.start("overall");
            onCreate(inputFileName);
            log.info("开始处理，总任务数：{}，线程数：{}", modelList.size(), config.getThreadCount());
            // 创建线程池
            ThreadPoolExecutor executor = ThreadPoolExecutorUtil.createThreadPool(config.getThreadCount());
            int totalSize = modelList.size();
            int batchSize = (int) Math.ceil((double) totalSize / config.getThreadCount());
            List<Future<?>> futures = new ArrayList<>();
            // 按批次提交任务
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
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("任务执行失败: {}", e.getMessage(), e);
                }
            }
            // 关闭线程池
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                log.warn("线程池未能在指定时间内关闭");
            }
            onDestroy();
            log.info("处理完成，总耗时：{}ms，成功：{}，失败：{}，总处理：{}", 
                timeInterval.intervalMs("overall"),
                successModelList.size(),
                failureModelList.size(),
                processedCount.get());
        } catch (Exception e) {
            log.error("系统执行异常: {}", e.getMessage(), e);
        }
    }

    private void processSingleModel(Model model, String threadName, ModuleEnum module, MenuEnum menu) {
        TimeInterval singleInterval = new TimeInterval();
        singleInterval.start("single");
        log.info("[{}] 开始处理记录: model={}", threadName, JSON.toJSONString(model));
        for (int retryCount = 1; retryCount <= config.getRetryCount(); retryCount++) {
            try {
                onStart(threadLocalPage.get(), model);
                addToSuccessList(model);
                log.info("[{}] 处理成功: model={}, 耗时: {}ms",
                    threadName, JSON.toJSONString(model), singleInterval.intervalMs("single"));
                break;
            } catch (Exception e) {
                log.warn("[{}] 处理失败: model={}, 重试次数: {}, 错误: {}",
                    threadName, JSON.toJSONString(model), retryCount, e.getMessage());
                
                if (retryCount == config.getRetryCount()) {
                    addToFailureList(model);
                    log.error("[{}] 最终处理失败: model={}, 错误: {}",
                        threadName, JSON.toJSONString(model), e.getMessage());
                } else {
                    retryOperation(threadName, module, menu);
                }
            }
        }
        processedCount.incrementAndGet();
        log.info("[{}] 处理完成: model={}, 总耗时: {}ms",
            threadName, JSON.toJSONString(model), singleInterval.intervalMs("single"));
    }

    private void retryOperation(String threadName, ModuleEnum module, MenuEnum menu) {
        try {
            threadLocalPage.get().reload();
            moduleHandler.openModule(threadLocalPage.get(), module);
            menuHandler.openMenu(threadLocalPage.get(), menu);
        } catch (Exception e) {
            log.error("[{}] 重试操作失败: {}", threadName, e.getMessage(), e);
        }
    }

    private void initPlaywright(String threadName) {
        Playwright playwright = Playwright.create();
        playwrightMap.put(threadName, playwright);
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        browserMap.put(threadName, browser);
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        page.setDefaultTimeout(config.getGlobalTimeout());
        threadLocalPage.set(page);
        page.navigate(config.getUrl());
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
        } catch (Exception e) {
            log.error("[{}] Error cleaning up Playwright resources", threadName, e);
        }
    }

    // 添加成功记录
    protected void addToSuccessList(Model model) {
        successModelList.add(model);
    }

    // 添加失败记录
    protected void addToFailureList(Model model) {
        failureModelList.add(model);
    }

    // 初始化程序
    protected void onCreate(String inputFileName) {
        modelList = new ArrayList<>();
        failureModelList.clear();
        successModelList.clear();
        processedCount.set(0);
        // 读取Excel文件
        InputStream inputStream = BaseActivity.class.getClassLoader().getResourceAsStream(inputFileName);
        EasyExcel.read(inputStream, Model.class, new PageReadListener<Model>(dataList -> modelList.addAll(dataList))).sheet().doRead();
        log.info("数据读取完成，总条数：{}", modelList.size());
    }

    // 抽象方法 onStart，由子类实现具体业务逻辑
    protected abstract void onStart(Page page, Model model);

    protected void onDestroy() {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String fileName = "target/output/" + date + "_result.xlsx";
        File directory = new File("target/output");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        // 对列表进行排序
        failureModelList.sort(Comparator.comparing(Model::getId));
        successModelList.sort(Comparator.comparing(Model::getId));
        // 使用同一个ExcelWriter写入多个sheet
        try (ExcelWriter excelWriter = EasyExcel.write(fileName, Model.class).build()) {
            // 写入失败数据
            WriteSheet failureSheet = EasyExcel.writerSheet(0, "失败数据").build();
            excelWriter.write(failureModelList, failureSheet);
            // 写入成功数据
            WriteSheet successSheet = EasyExcel.writerSheet(1, "成功数据").build();
            excelWriter.write(successModelList, successSheet);
            // 写入全部数据
            WriteSheet allSheet = EasyExcel.writerSheet(2, "全部数据").build();
            excelWriter.write(modelList, allSheet);
        }
        log.info("结果文件已生成: {}", fileName);
    }
}