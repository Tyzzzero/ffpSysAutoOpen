package org.example.service;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.*;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.controller.MenuHandler;
import org.example.controller.ModuleHandler;
import org.example.entity.MenuEnum;
import org.example.entity.Model;
import org.example.entity.ModuleEnum;
import org.example.utils.CaptchaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Tyzzzero
 * 抽象类 BaseActivity
 */
public abstract class BaseActivity {
    private static final Config config = ConfigLoader.loadConfig(Config.class);
    private static final Logger log = LoggerFactory.getLogger(BaseActivity.class);
    protected static Page page;
    protected static List<Model> modelList;
    protected static List<Model> failureModelList;
    protected static List<Model> successModelList;
    private final ModuleHandler moduleHandler = new ModuleHandler();
    private final MenuHandler menuHandler = new MenuHandler();


    // 执行方法
    public void execute(String inputFileName, ModuleEnum module, MenuEnum menu) {
        try {
            TimeInterval timeInterval = new TimeInterval();
            timeInterval.start("overall");
            onCreate(inputFileName);
            for (int index = 0; index < modelList.size(); index++) {
                timeInterval.start("single");
                Model model = modelList.get(index);
                log.info("start-----");
                log.info("对象：{}", JSON.toJSONString(model));
                for (int i = 1; i <= config.getRetryCount(); i++) {
                    try {
                        if (index == 0) {
                            login();
                            moduleHandler.openModule(page, module);
                            menuHandler.openMenu(page, menu);
                        }
                        onStart(page, model);
                        successModelList.add(model);
                        log.info("执行成功");
                        log.info("end-------");
                        break;
                    } catch (PlaywrightException e) {
                        if (e.getMessage().contains("Timeout")) {
                            log.warn("超时重试，次数：{}", i);
                            page.reload();
                            moduleHandler.openModule(page, module);
                            menuHandler.openMenu(page, menu);
                            if (i == config.getRetryCount()) {
                                failureModelList.add(model);
                                log.error("执行失败，对象：{}", JSON.toJSONString(model));
                            }
                            continue;
                        }
                        throw e;
                    }
                }
                log.info("耗时：{}", timeInterval.intervalRestart("single"));
            }
            outputResult();
            log.info("总耗时：{}", timeInterval.intervalSecond("overall"));
        } catch (Exception e) {
            log.error("执行异常：{}", e.getStackTrace());
        }
    }


    // 初始化程序
    protected static void onCreate(String inputFileName) {
        modelList = new LinkedList<>();
        failureModelList = new LinkedList<>();
        successModelList = new LinkedList<>();
        InputStream inputStream = BaseActivity.class.getClassLoader().getResourceAsStream(inputFileName);
        EasyExcel.read(inputStream, Model.class, new PageReadListener<Model>(modelList::addAll)).sheet().doRead();
        log.info("读取表格行数：{}", modelList.size());
        Playwright playwright = Playwright.create();
        BrowserContext context;
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(config.getGlobalTimeout());
    }


    // 抽象方法 onStart，由子类实现具体业务逻辑
    protected abstract void onStart(Page page, Model model);


    // 登录方法
    protected static void login() {
        page.navigate(config.getUrl());
        page.locator("#username").click();
        page.locator("#username").fill(config.getUserName());
        page.locator("#password").click();
        page.locator("#password").fill(config.getPassword());
        Locator captchaImageLocator = page.locator("#jcaptcha");
        captchaImageLocator.click();
        String targetPath = System.getProperty("user.dir") + File.separator + "target";
        File captchaImageFile = new File(targetPath + File.separator + "captcha.png");
        captchaImageLocator.screenshot(new Locator.ScreenshotOptions().setPath(Paths.get(captchaImageFile.getAbsolutePath())));
        page.locator("#jcaptcha_response").click();
        page.locator("#jcaptcha_response").fill(CaptchaUtil.recognizeCaptcha(captchaImageFile.getAbsolutePath()));
        page.locator("#jcaptcha_response").press("Enter");
    }


    // 输出结果
    protected static void outputResult() {
        String outputDirPath = System.getProperty("user.dir") + "/target/output";
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try {
            String dateStr = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            File failureOutputFile = new File(outputDirPath + "/" + dateStr + "_failure" + ".xlsx");
            OutputStream failureOutputStream = new FileOutputStream(failureOutputFile);
            EasyExcel.write(failureOutputStream).head(Model.class).sheet("失败").doWrite(failureModelList);
            failureOutputStream.close();
            File successOutputFile = new File(outputDirPath + "/" + dateStr + "_success" + ".xlsx");
            OutputStream successOutputStream = new FileOutputStream(successOutputFile);
            EasyExcel.write(successOutputStream).head(Model.class).sheet("完成").doWrite(successModelList);
            successOutputStream.close();
            File allOutputFile = new File(outputDirPath + "/" + dateStr + "_all" + ".xlsx");
            OutputStream allOutputStream = new FileOutputStream(allOutputFile);
            EasyExcel.write(allOutputStream).head(Model.class).sheet("全部").doWrite(modelList);
            successOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("成功条数：{}，失败条数：{}，处理条数：{}，读取条数：{}", successModelList.size(), failureModelList.size(), successModelList.size() + failureModelList.size(), modelList.size());
        if ((successModelList.size() + failureModelList.size())!= modelList.size()) {
            log.error("处理条数与读取条数不一致");
        }
    }
}