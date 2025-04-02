package org.example.utils;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Tyzzzero
 */
public class CaptchaUtil {

    private static final String TESSDATA_PATH = "src/main/resources/traineddata";
    private static final Logger log = LoggerFactory.getLogger(CaptchaUtil.class);

    /**
     * 封装的方法，用于识别验证码图片并返回识别结果
     *
     * @param captchaImagePath 验证码图片的完整路径
     * @return 识别出的验证码内容，如果识别失败返回空字符串
     */
    public static String recognizeCaptcha(String captchaImagePath) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(TESSDATA_PATH);
        try {
            File captchaImageFile = new File(captchaImagePath);
            if (!captchaImageFile.exists()) {
                log.error("验证码指定的验证码图片不存在");
                return "";
            }
            String result = tesseract.doOCR(captchaImageFile);
            log.info("识别验证码：{}", result);
            return result;
        } catch (TesseractException e) {
            log.error("Tesseract异常：{}", e.getMessage());
            return "";
        }
    }
}
