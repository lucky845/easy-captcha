package com.lucky845.basic.core.captcha.impl;

import com.lucky845.basic.core.captcha.AbstractCaptcha;
import com.lucky845.basic.core.enums.ContentTypeEnum;
import com.lucky845.basic.core.exception.CaptchaGeneratorException;
import com.lucky845.basic.core.utils.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * todo 滑块验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class SliderCaptcha extends AbstractCaptcha {

    private final int sliderPosition; // 滑块正确位置

    public SliderCaptcha(int width, int height) {
        this.captchaWidth = width;
        this.captchaHeight = height;
        this.sliderPosition = generateSliderPosition();
    }

    /**
     * 生成滑块正确位置
     *
     * @return 滑块正确位置
     */
    private int generateSliderPosition() {
        return RandomUtils.num(50, captchaWidth - 50); // 随机生成滑块位置
    }

    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        BufferedImage image = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);

        // 绘制滑块轨道
        g2d.setColor(Color.GRAY);
        g2d.fillRect(10, captchaHeight / 2 - 5, captchaWidth - 20, 10);

        // 绘制滑块
        g2d.setColor(Color.BLUE);
        g2d.fillRect(sliderPosition, captchaHeight / 2 - 15, 30, 30);

        g2d.dispose();

        // 将图像写入输出流
        try {
            return ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new CaptchaGeneratorException("生成滑块验证码失败", e);
        }
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.PNG;
    }

    /**
     * 校验滑块位置
     *
     * @param position 用户拖动的位置
     * @return 是否正确
     */
    public boolean checkSliderPosition(int position) {
        return Math.abs(position - sliderPosition) <= 5; // 允许 5 像素的误差
    }
}
