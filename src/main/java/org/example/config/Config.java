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
    
    // 系统URL
    private String url;
    
    // 登录用户名
    private String userName;
    
    // 登录密码
    private String password;
    
    // 乡镇名称
    private String town;
    
    // 重试次数
    private int retryCount = 3;
    
    // 全局超时时间（毫秒）
    private double globalTimeout = 8000.0;
    
    // 线程数
    private int threadCount = 3;
    
    // 验证码最大尝试次数
    private int maxCaptchaAttempts = 5;
    
    public void setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("系统URL不能为空");
        }
        this.url = url;
    }
    
    public void setUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("登录用户名不能为空");
        }
        this.userName = userName;
    }
    
    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("登录密码不能为空");
        }
        this.password = password;
    }
    
    public void setRetryCount(int retryCount) {
        if (retryCount < 1) {
            log.warn("重试次数不能小于1，使用默认值3");
            this.retryCount = 3;
        } else {
            this.retryCount = retryCount;
        }
    }
    
    public void setGlobalTimeout(double globalTimeout) {
        if (globalTimeout < 1000) {
            log.warn("超时时间不能小于1000ms，使用默认值8000ms");
            this.globalTimeout = 8000.0;
        } else {
            this.globalTimeout = globalTimeout;
        }
    }
    
    public void setThreadCount(int threadCount) {
        if (threadCount < 1) {
            log.warn("线程数不能小于1，使用默认值1");
            this.threadCount = 3;
        } else if (threadCount > 10) {
            log.warn("线程数不能大于10，使用默认值1");
            this.threadCount = 1;
        } else {
            this.threadCount = threadCount;
        }
    }
    
    public void setMaxCaptchaAttempts(int maxCaptchaAttempts) {
        if (maxCaptchaAttempts < 1) {
            log.warn("验证码最大尝试次数不能小于1，使用默认值5");
            this.maxCaptchaAttempts = 5;
        } else if (maxCaptchaAttempts > 10) {
            log.warn("验证码最大尝试次数不能大于10，使用默认值5");
            this.maxCaptchaAttempts = 5;
        } else {
            this.maxCaptchaAttempts = maxCaptchaAttempts;
        }
    }
    
    /**
     * 验证必填配置项
     * @throws IllegalStateException 如果必填项未配置
     */
    public void validate() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("系统URL未配置");
        }
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalStateException("登录用户名未配置");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("登录密码未配置");
        }
    }
}