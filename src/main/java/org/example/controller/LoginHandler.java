package org.example.controller;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.utils.CaptchaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

/**
 * 登录处理类
 */
public class LoginHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);
    private final Config config = ConfigLoader.loadConfig(Config.class);

    public void login(Page page) {
        try {
            page.locator("#username").click();
            page.locator("#username").fill(config.getUserName());
            page.locator("#password").click();
            page.locator("#password").fill(config.getPassword());
            
            Locator captchaImageLocator = page.locator("#jcaptcha");
            captchaImageLocator.click();
            
            String targetPath = "target";
            File directory = new File(targetPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            String threadName = Thread.currentThread().getName();
            File captchaImageFile = new File(targetPath + File.separator + "captcha_" + threadName + ".png");
            captchaImageLocator.screenshot(new Locator.ScreenshotOptions()
                .setPath(Paths.get(captchaImageFile.getAbsolutePath())));
            
            String captchaText = CaptchaUtil.recognizeCaptcha(captchaImageFile.getAbsolutePath());
            page.locator("#jcaptcha_response").click();
            page.locator("#jcaptcha_response").fill(captchaText);
            page.locator("#jcaptcha_response").press("Enter");
            
            // 等待登录完成
            page.waitForURL("**/portal/techcomp/idm/indexsys.jsp");
            log.info("[{}] Login successful", threadName);
        } catch (Exception e) {
            log.error("[{}] Login failed: {}", Thread.currentThread().getName(), e.getMessage());
            throw e;
        }
    }
} 