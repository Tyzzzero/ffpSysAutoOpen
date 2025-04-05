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

            int attempts = 0;
            boolean loginSuccess = false;
            String threadName = Thread.currentThread().getName();

            while (attempts < config.getMaxCaptchaAttempts() && !loginSuccess) {
                attempts++;
                log.info("[{}] 尝试验证码验证，第{}次", threadName, attempts);

                Locator captchaImageLocator = page.locator("#jcaptcha");
                captchaImageLocator.click();

                String targetPath = "target";
                File directory = new File(targetPath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File captchaImageFile = new File(targetPath + File.separator + "captcha_" + threadName + ".png");
                captchaImageLocator.screenshot(new Locator.ScreenshotOptions()
                        .setPath(Paths.get(captchaImageFile.getAbsolutePath())));

                String captchaText = CaptchaUtil.recognizeCaptcha(captchaImageFile.getAbsolutePath());
                page.locator("#jcaptcha_response").click();
                page.locator("#jcaptcha_response").fill(captchaText);
                page.locator("#jcaptcha_response").press("Enter");

                try {
                    // 等待登录完成或错误提示
                    page.waitForURL("**/portal/techcomp/idm/indexsys.jsp", new Page.WaitForURLOptions().setTimeout(5000));
                    loginSuccess = true;
                    log.info("[{}] 登录成功", threadName);
                } catch (Exception e) {
                    if (attempts < config.getMaxCaptchaAttempts()) {
                        log.warn("[{}] 验证码验证失败，准备重试", threadName);
                        // 清空验证码输入框
                        page.locator("#jcaptcha_response").click();
                        page.locator("#jcaptcha_response").fill("");
                    } else {
                        log.error("[{}] 验证码验证次数超过最大限制{}次，登录失败", threadName, config.getMaxCaptchaAttempts());
                        throw new RuntimeException("验证码验证失败次数过多");
                    }
                }
            }
        } catch (Exception e) {
            log.error("登录异常：{}", e.getMessage());
            throw new RuntimeException("登录失败：" + e.getMessage());
        }
    }
} 