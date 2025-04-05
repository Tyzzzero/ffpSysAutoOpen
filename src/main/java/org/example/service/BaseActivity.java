package org.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import com.alibaba.excel.EasyExcel;
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

/**
 * @author Tyzzzero
 * 抽象类 BaseActivity
 */
public abstract class BaseActivity {
    private static final Config config = ConfigLoader.loadConfig(Config.class);
    private static final Logger log = LoggerFactory.getLogger(BaseActivity.class);
    protected static List<Model> modelList;
    protected static List<Model> failureModelList;
    protected static List<Model> successModelList;
    private final ModuleHandler moduleHandler = new ModuleHandler();
    private final MenuHandler menuHandler = new MenuHandler();
    private final LoginHandler loginHandler = new LoginHandler();
    private static final ThreadLocal<Page> threadLocalPage = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, Playwright> playwrightMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Browser> browserMap = new ConcurrentHashMap<>();

    // 执行方法
    public void execute(String inputFileName, ModuleEnum module, MenuEnum menu) {
        try {
            TimeInterval timeInterval = new TimeInterval();
            timeInterval.start("overall");
            onCreate(inputFileName);

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
                        // 为每个线程创建独立的Playwright实例
                        initPlaywright(threadName);
                        loginHandler.login(threadLocalPage.get());
                        moduleHandler.openModule(threadLocalPage.get(), module);
                        menuHandler.openMenu(threadLocalPage.get(), menu);
                        for (Model model : subList) {
                            TimeInterval singleInterval = new TimeInterval();
                            singleInterval.start("single");
                            log.info("[{}] start-----", threadName);
                            log.info("[{}] 对象：{}", threadName, JSON.toJSONString(model));
                            
                            for (int retryCount = 1; retryCount <= config.getRetryCount(); retryCount++) {
                                try {
                                    onStart(threadLocalPage.get(), model);
                                    synchronized (successModelList) {
                                        successModelList.add(model);
                                    }
                                    break;
                                } catch (Exception e) {
                                    log.error("[{}] 异常信息：{}", threadName, e.getMessage());
                                    log.warn("重试次数：{}", retryCount);
                                    threadLocalPage.get().reload();
                                    moduleHandler.openModule(threadLocalPage.get(), module);
                                    menuHandler.openMenu(threadLocalPage.get(), menu);
                                    if (retryCount == config.getRetryCount()) {
                                        synchronized (failureModelList) {
                                            failureModelList.add(model);
                                        }
                                    }
                                }
                            }
                            log.info("[{}] 单条记录耗时：{}ms", threadName, singleInterval.intervalSecond("single"));
                            log.info("[{}] end-----", threadName);
                        }
                    } finally {
                        // 清理资源
                        cleanupPlaywright(threadName);
                    }
                }));
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Task execution failed", e);
                }
            }

            // 关闭线程池
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            onDestroy();
            log.info("总耗时：{}ms", timeInterval.intervalMs("overall"));
        } catch (Exception e) {
            log.error("执行异常", e);
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

    // 初始化程序
    protected void onCreate(String inputFileName) {
        modelList = new ArrayList<>();
        failureModelList = new ArrayList<>();
        successModelList = new ArrayList<>();
        
        // 读取Excel文件
        InputStream inputStream = BaseActivity.class.getClassLoader().getResourceAsStream(inputFileName);
        EasyExcel.read(inputStream, Model.class, new PageReadListener<Model>(dataList -> modelList.addAll(dataList))).sheet().doRead();
        log.info("读取完成，总条数：{}", modelList.size());
    }

    // 抽象方法 onStart，由子类实现具体业务逻辑
    protected abstract void onStart(Page page, Model model);

    protected void onDestroy() {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String allFileName = "target/output/" + date + "_all.xlsx";
        String failureFileName = "target/output/" + date + "_failure.xlsx";
        String successFileName = "target/output/" + date + "_success.xlsx";
        File directory = new File("target/output");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        EasyExcel.write(allFileName, Model.class).sheet("sheet1").doWrite(modelList);
        EasyExcel.write(failureFileName, Model.class).sheet("sheet1").doWrite(failureModelList);
        EasyExcel.write(successFileName, Model.class).sheet("sheet1").doWrite(successModelList);
        log.info("执行完成，成功条数：{}，失败条数：{}，处理条数：{}，读取条数：{}", 
            successModelList.size(), failureModelList.size(), 
            successModelList.size() + failureModelList.size(), modelList.size());
        if ((successModelList.size() + failureModelList.size()) != modelList.size()) {
            log.error("处理条数与读取条数不一致");
        }
    }
}