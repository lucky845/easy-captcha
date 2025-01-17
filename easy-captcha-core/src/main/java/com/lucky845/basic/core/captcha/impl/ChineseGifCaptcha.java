package com.lucky845.basic.core.captcha.impl;

import com.lucky845.basic.core.captcha.AbstractGifCaptcha;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 中文 GIF 验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ChineseGifCaptcha extends AbstractGifCaptcha {

    public ChineseGifCaptcha() {
    }

    public ChineseGifCaptcha(int width, int height) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
    }

    public ChineseGifCaptcha(int width, int height, int len) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
    }

    public ChineseGifCaptcha(int width, int height, int len, Font font) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
        setFont(font);
    }

    @Override
    protected void generateFrames() {
        clearFrames(); // 清空帧列表

        // 生成多帧图像
        for (int i = 0; i < 10; i++) { // 假设生成 10 帧
            BufferedImage frame = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = frame.createGraphics();

            // 设置背景
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, captchaWidth, captchaHeight);

            // 绘制中文验证码
            g2d.setColor(Color.BLACK);
            g2d.setFont(getFont());
            g2d.drawString(generateChineseCaptcha(), 10, captchaHeight / 2);

            // 添加干扰元素
            drawLine(5, g2d);
            drawOval(5, g2d);

            g2d.dispose();
            addFrame(frame); // 添加帧
        }
    }

}
