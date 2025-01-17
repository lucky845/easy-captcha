package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptcha;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import static io.github.lucky845.basic.core.utils.RandomUtils.num;

/**
 * 算数类型验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ArithmeticCaptcha extends AbstractCaptcha {

    public ArithmeticCaptcha() {
    }

    public ArithmeticCaptcha(int width, int height) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
    }

    public ArithmeticCaptcha(int width, int height, int len) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
    }

    public ArithmeticCaptcha(int width, int height, int len, Font font) {
        this();
        setCaptchaWidth(width);
        setCaptchaHeight(height);
        setCaptchaLength(len);
        setFont(font);
    }

    /**
     * 生成算数表达式
     *
     * @return 算数表达式
     */
    private String generateArithmeticExpression() {
        int num1 = num(1, 10);
        int num2 = num(1, 10);
        char[] arithmetic = {'+', '-', '*'};
        char operator = arithmetic[num(arithmetic.length)];
        this.captcha = calculateResult(num1, num2, operator) + "";
        return String.format("%d %c %d = ?", num1, operator, num2);
    }

    /**
     * 计算结果
     *
     * @param num1     第一个数字
     * @param num2     第二个数字
     * @param operator 运算符
     * @return 计算结果
     */
    private int calculateResult(int num1, int num2, char operator) {
        return switch (operator) {
            case '+' -> num1 + num2;
            case '-' -> num1 - num2;
            case '*' -> num1 * num2;
            default -> throw new IllegalArgumentException("不支持的运算符: " + operator);
        };
    }

    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        BufferedImage image = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);

        // 绘制算数表达式
        g2d.setColor(Color.BLACK);
        g2d.setFont(getFont());
        g2d.drawString(generateArithmeticExpression(), 10, captchaHeight / 2);

        // 添加干扰元素
        drawLine(5, g2d);
        drawOval(5, g2d);

        g2d.dispose();

        // 将图像写入输出流
        try {
            return ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new CaptchaGeneratorException("生成算数验证码失败", e);
        }
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.PNG;
    }
}
