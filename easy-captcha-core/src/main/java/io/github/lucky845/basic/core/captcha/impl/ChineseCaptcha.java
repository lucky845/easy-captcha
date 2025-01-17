package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptcha;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 中文验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ChineseCaptcha extends AbstractCaptcha {

    public ChineseCaptcha() {
    }

    public ChineseCaptcha(int width, int height) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
    }

    public ChineseCaptcha(int width, int height, int len) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
    }

    public ChineseCaptcha(int width, int height, int len, Font font) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
        setFont(font);
    }

    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        BufferedImage image = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 添加干扰元素
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        drawLine(5, g2d);
        drawOval(5, g2d);

        // 绘制中文字符
        g2d.setColor(Color.BLACK);
        g2d.setFont(getFont());
        g2d.drawString(generateChineseCaptcha(), 10, captchaHeight / 2);

        g2d.dispose();

        // 将图像写入输出流
        try {
            return ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new CaptchaGeneratorException("生成中文验证码失败", e);
        }
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.PNG;
    }
}
