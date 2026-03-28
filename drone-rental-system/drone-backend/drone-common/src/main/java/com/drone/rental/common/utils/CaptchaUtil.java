package com.drone.rental.common.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * 验证码工具类
 */
public class CaptchaUtil {

    private static final String CHAR_POOL = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final Random RANDOM = new Random();

    /**
     * 验证码类
     */
    public static class Captcha {
        private String code;
        private String imageBase64;

        public Captcha(String code, String imageBase64) {
            this.code = code;
            this.imageBase64 = imageBase64;
        }

        public String getCode() {
            return code;
        }

        public String getImageBase64() {
            return imageBase64;
        }
    }

    /**
     * 生成验证码
     */
    public static Captcha generateCaptcha() {
        // 创建BufferedImage对象
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();

        // 填充背景色
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        // 生成验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            String charStr = CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())) + "";
            code.append(charStr);
            // 随机颜色
            graphics.setColor(getRandomColor(50, 200));
            // 随机字体
            graphics.setFont(getRandomFont());
            // 绘制字符
            graphics.drawString(charStr, 20 * i + 15, 28);
        }

        // 添加干扰线
        for (int i = 0; i < 6; i++) {
            graphics.setColor(getRandomColor(100, 200));
            graphics.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 添加干扰点
        for (int i = 0; i < 50; i++) {
            graphics.setColor(getRandomColor(150, 220));
            graphics.drawOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 1, 1);
        }

        graphics.dispose();

        // 转换为Base64
        String imageBase64 = imageToBase64(image);

        return new Captcha(code.toString(), imageBase64);
    }

    /**
     * 获取随机颜色
     */
    private static Color getRandomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }

    /**
     * 获取随机字体
     */
    private static Font getRandomFont() {
        String[] fonts = {"Arial", "Courier New", "Verdana", "Times New Roman"};
        String fontName = fonts[RANDOM.nextInt(fonts.length)];
        int style = RANDOM.nextInt(2); // 0: plain, 1: bold
        int size = 20 + RANDOM.nextInt(5);
        return new Font(fontName, style, size);
    }

    /**
     * 图片转Base64
     */
    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
