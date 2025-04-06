package org.example.utils;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tyzzzero
 */
public class CaptchaUtil {
    private static final String TESSDATA_PATH = "src/main/resources/traineddata";
    private static final Logger log = LoggerFactory.getLogger(CaptchaUtil.class);
    private static final int EXPECTED_LENGTH = 4;
    private static String cachedResult = null;
    private static final int RECOGNITION_ATTEMPTS = 3;
    private static final float VOTE_THRESHOLD = 0.5f;

    /**
     * 封装的方法，用于识别验证码图片并返回识别结果
     */
    public static String recognizeCaptcha(String captchaImagePath) {
        String threadName = Thread.currentThread().getName();
        try {
            File captchaImageFile = new File(captchaImagePath);
            if (!captchaImageFile.exists()) {
                log.error("[{}] 验证码图片不存在: {}", threadName, captchaImagePath);
                return "";
            }

            log.info("[{}] 开始验证码识别...", threadName);
            Map<String, Integer> resultVotes = new HashMap<>();
            BufferedImage originalImage = ImageIO.read(captchaImageFile);

            for (int attempt = 0; attempt < RECOGNITION_ATTEMPTS; attempt++) {
                BufferedImage processedImage = preprocessImage(originalImage, attempt);
                File processedFile = new File(captchaImagePath + "_processed_" + attempt + ".png");
                ImageIO.write(processedImage, "png", processedFile);

                try {
                    String result = performOCR(processedFile);
                    String cleanResult = extractAndValidateDigits(result);
                    if (!cleanResult.isEmpty()) {
                        resultVotes.merge(cleanResult, 1, Integer::sum);
                        log.debug("[{}] 第{}次识别结果: {}", threadName, attempt + 1, cleanResult);
                    }
                } finally {
                    processedFile.delete();
                }
            }

            // 输出投票统计信息
            if (!resultVotes.isEmpty()) {
                StringBuilder voteInfo = new StringBuilder();
                voteInfo.append(String.format("[%s] 投票统计 => ", threadName));
                resultVotes.forEach((result, votes) -> 
                    voteInfo.append(String.format("[%s: %d票] ", result, votes)));
                log.info(voteInfo.toString());
            } else {
                log.warn("[{}] 本次识别没有有效结果", threadName);
            }

            String bestResult = getBestVotedResult(resultVotes);
            if (!bestResult.isEmpty()) {
                cachedResult = bestResult;
                log.info("[{}] 验证码识别成功 ✓ 结果: {}", threadName, bestResult);
                return bestResult;
            } else if (cachedResult != null) {
                log.info("[{}] 使用缓存结果 ⚡ 结果: {}", threadName, cachedResult);
                return cachedResult;
            }

            log.warn("[{}] 验证码识别失败 ✗", threadName);
            return "";
        } catch (IOException e) {
            log.error("[{}] 验证码识别异常: {}", threadName, e.getMessage());
            return cachedResult != null ? cachedResult : "";
        }
    }

    /**
     * 图像预处理，根据尝试次数使用不同的处理参数
     */
    private static BufferedImage preprocessImage(BufferedImage original, int attempt) {
        String threadName = Thread.currentThread().getName();
        log.debug("[{}] 开始图像预处理 - 处理方案 {}", threadName, attempt + 1);

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. 放大图像
        BufferedImage scaledImage = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, width * 2, height * 2, null);
        g2d.dispose();

        BufferedImage processedImage = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(), 
                                                       BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = processedImage.getGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        switch (attempt) {
            case 0:
                processedImage = enhanceContrast(processedImage, 128, 30);
                break;
            case 1:
                processedImage = enhanceContrast(processedImage, 120, 40);
                processedImage = applySharpening(processedImage);
                break;
            case 2:
                processedImage = enhanceContrast(processedImage, 135, 25);
                processedImage = applyNoiseReduction(processedImage);
                break;
        }

        return processedImage;
    }

    /**
     * 对比度增强
     */
    private static BufferedImage enhanceContrast(BufferedImage image, int threshold, int adjustment) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xff;

                if (gray < threshold) {
                    gray = Math.max(0, gray - adjustment);
                } else {
                    gray = Math.min(255, gray + adjustment);
                }

                int newRGB = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, newRGB);
            }
        }
        
        return result;
    }

    /**
     * 锐化处理
     */
    private static BufferedImage applySharpening(BufferedImage image) {
        float[] sharpen = {
            0.0f, -1.0f, 0.0f,
            -1.0f, 5.0f, -1.0f,
            0.0f, -1.0f, 0.0f
        };
        
        Kernel kernel = new Kernel(3, 3, sharpen);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }

    /**
     * 降噪处理
     */
    private static BufferedImage applyNoiseReduction(BufferedImage image) {
        float[] blur = {
            1/9f, 1/9f, 1/9f,
            1/9f, 1/9f, 1/9f,
            1/9f, 1/9f, 1/9f
        };
        
        Kernel kernel = new Kernel(3, 3, blur);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }

    /**
     * 执行OCR识别
     */
    private static String performOCR(File imageFile) throws IOException {
        String threadName = Thread.currentThread().getName();
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(TESSDATA_PATH);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789");
            tesseract.setVariable("tessedit_pageseg_mode", "7");
            tesseract.setVariable("user_defined_dpi", "300");
            return tesseract.doOCR(imageFile).trim();
        } catch (TesseractException e) {
            log.error("[{}] OCR识别失败: {}", threadName, e.getMessage());
            return "";
        }
    }

    /**
     * 提取并验证数字
     */
    private static String extractAndValidateDigits(String result) {
        String threadName = Thread.currentThread().getName();
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(result);
        StringBuilder digits = new StringBuilder();
        while (matcher.find() && digits.length() < EXPECTED_LENGTH) {
            digits.append(matcher.group());
        }

        String cleanResult = digits.toString();
        if (cleanResult.length() == EXPECTED_LENGTH) {
            return cleanResult;
        }

        log.debug("[{}] 验证码格式不符: 期望{}位数字，实际为{}位", 
                 threadName, EXPECTED_LENGTH, cleanResult.length());
        return "";
    }

    /**
     * 获取投票最多的结果
     */
    private static String getBestVotedResult(Map<String, Integer> votes) {
        String threadName = Thread.currentThread().getName();
        if (votes.isEmpty()) {
            return "";
        }

        Map.Entry<String, Integer> maxEntry = Collections.max(votes.entrySet(), Map.Entry.comparingByValue());
        int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
        float voteRate = (float) maxEntry.getValue() / totalVotes;

        log.debug("[{}] 最高票结果: {} (得票率: {}%)",
                 threadName, maxEntry.getKey(), voteRate * 100);

        if (voteRate >= VOTE_THRESHOLD) {
            return maxEntry.getKey();
        }

        return "";
    }

    /**
     * 清除缓存的结果
     */
    public static void clearCache() {
        String threadName = Thread.currentThread().getName();
        if (cachedResult != null) {
            log.debug("[{}] 清除缓存结果: {}", threadName, cachedResult);
            cachedResult = null;
        }
    }
}
