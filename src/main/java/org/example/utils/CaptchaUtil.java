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
    private static final int RECOGNITION_ATTEMPTS = 3; // 每次识别尝试次数
    private static final float VOTE_THRESHOLD = 0.5f; // 投票阈值

    /**
     * 封装的方法，用于识别验证码图片并返回识别结果
     *
     * @param captchaImagePath 验证码图片的完整路径
     * @return 识别出的验证码内容，如果识别失败返回空字符串
     */
    public static String recognizeCaptcha(String captchaImagePath) {
        try {
            // 1. 验证图片是否存在
            File captchaImageFile = new File(captchaImagePath);
            if (!captchaImageFile.exists()) {
                log.error("验证码指定的验证码图片不存在");
                return "";
            }
            // 多次识别投票
            Map<String, Integer> resultVotes = new HashMap<>();
            BufferedImage originalImage = ImageIO.read(captchaImageFile);
            for (int attempt = 0; attempt < RECOGNITION_ATTEMPTS; attempt++) {
                // 每次使用不同的预处理参数
                BufferedImage processedImage = preprocessImage(originalImage, attempt);
                File processedFile = new File(captchaImagePath + "_processed_" + attempt + ".png");
                ImageIO.write(processedImage, "png", processedFile);
                try {
                    String result = performOCR(processedFile);
                    String cleanResult = extractAndValidateDigits(result);
                    if (!cleanResult.isEmpty()) {
                        resultVotes.merge(cleanResult, 1, Integer::sum);
                    }
                } finally {
                    processedFile.delete();
                }
            }
            // 统计投票结果
            String bestResult = getBestVotedResult(resultVotes);
            if (!bestResult.isEmpty()) {
                cachedResult = bestResult;
                log.info("验证码识别成功，结果：{}", bestResult);
                return bestResult;
            } else if (cachedResult != null) {
                log.info("使用缓存的识别结果：{}", cachedResult);
                return cachedResult;
            }
            return "";
        } catch (IOException e) {
            log.error("验证码识别异常：{}", e.getMessage());
            return cachedResult != null ? cachedResult : "";
        }
    }

    /**
     * 图像预处理，根据尝试次数使用不同的处理参数
     */
    private static BufferedImage preprocessImage(BufferedImage original, int attempt) {
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

        // 2. 根据尝试次数使用不同的预处理参数
        BufferedImage processedImage = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(), 
                                                       BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = processedImage.getGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        // 3. 应用不同的图像增强
        switch (attempt) {
            case 0: // 标准处理
                processedImage = enhanceContrast(processedImage, 128, 30);
                break;
            case 1: // 高对比度处理
                processedImage = enhanceContrast(processedImage, 120, 40);
                processedImage = applySharpening(processedImage);
                break;
            case 2: // 降噪处理
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
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(TESSDATA_PATH);
            tesseract.setVariable("tessedit_char_whitelist", "0123456789");
            tesseract.setVariable("tessedit_pageseg_mode", "7");
            // 设置DPI提高识别精度
            tesseract.setVariable("user_defined_dpi", "300");
            return tesseract.doOCR(imageFile).trim();
        } catch (TesseractException e) {
            log.error("OCR识别失败：{}", e.getMessage());
            return "";
        }
    }

    /**
     * 提取并验证数字
     */
    private static String extractAndValidateDigits(String result) {
        // 1. 提取所有数字
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(result);
        StringBuilder digits = new StringBuilder();
        while (matcher.find() && digits.length() < EXPECTED_LENGTH) {
            digits.append(matcher.group());
        }

        // 2. 验证结果
        String cleanResult = digits.toString();
        if (cleanResult.length() == EXPECTED_LENGTH) {
            return cleanResult;
        }

        log.warn("验证码格式不符合要求：{}", result);
        return "";
    }

    /**
     * 获取投票最多的结果
     */
    private static String getBestVotedResult(Map<String, Integer> votes) {
        if (votes.isEmpty()) {
            return "";
        }

        // 找出得票最多的结果
        Map.Entry<String, Integer> maxEntry = Collections.max(votes.entrySet(), Map.Entry.comparingByValue());
        int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
        
        // 计算得票率
        float voteRate = (float) maxEntry.getValue() / totalVotes;
        
        // 只有当得票率超过阈值时才返回结果
        if (voteRate >= VOTE_THRESHOLD) {
            return maxEntry.getKey();
        }
        
        return "";
    }

    /**
     * 清除缓存的结果
     */
    public static void clearCache() {
        cachedResult = null;
    }
}
