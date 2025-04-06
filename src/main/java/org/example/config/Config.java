package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统配置类
 * @author Tyzzzero
 */
@Getter
@Setter
public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    
    @Required("系统URL")
    private String url;
    
    @Required("登录用户名")
    private String userName;
    
    @Required("登录密码")
    private String password;
    
    // 乡镇名称
    private String town;
    
    // 重试次数
    private int retryCount = 3;
    
    // 全局超时时间（毫秒）
    private double globalTimeout = 8000.0;
    
    // 线程数
    private int threadCount = 1;
    
    // 验证码最大尝试次数
    private int maxCaptchaAttempts = 5;
    
    /**
     * 验证必填配置项
     * @throws IllegalStateException 如果必填项未配置
     */
    public void validate() {
        // 验证必填项
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("系统URL未配置");
        }
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalStateException("登录用户名未配置");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("登录密码未配置");
        }

        // 验证数值范围
        if (threadCount < 1) {
            log.warn("线程数不能小于1，使用默认值1");
            threadCount = 1;
        } else if (threadCount > 10) {
            log.warn("线程数不能大于10，使用默认值1");
            threadCount = 1;
        }

        if (retryCount < 1) {
            log.warn("重试次数不能小于1，使用默认值3");
            retryCount = 3;
        }

        if (globalTimeout < 1000) {
            log.warn("超时时间不能小于1000ms，使用默认值30000ms");
            globalTimeout = 30000;
        }

        if (maxCaptchaAttempts < 1) {
            log.warn("验证码最大尝试次数不能小于1，使用默认值5");
            maxCaptchaAttempts = 5;
        } else if (maxCaptchaAttempts > 10) {
            log.warn("验证码最大尝试次数不能大于10，使用默认值5");
            maxCaptchaAttempts = 5;
        }
    }
}