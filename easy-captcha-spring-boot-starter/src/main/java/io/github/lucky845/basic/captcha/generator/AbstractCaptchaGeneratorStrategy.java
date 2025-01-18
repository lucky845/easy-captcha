package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import jakarta.annotation.Resource;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 验证码生成抽象类
 *
 * @author created by lucky845 on 2025-01-16
 */
public abstract class AbstractCaptchaGeneratorStrategy {

    @Resource
    protected CaptchaProperties captchaProperties;

    /**
     * 生成验证码
     *
     * @param out 输出流
     * @throws IOException         IO异常
     * @throws FontFormatException 字体格式化异常
     */
    public abstract void generateCaptcha(OutputStream out) throws IOException, FontFormatException;

    /**
     * 生成Base64验证码
     *
     * @return 验证码Base64
     * @throws IOException         IO异常
     * @throws FontFormatException 字体格式话异常
     */
    public abstract String generateCaptchaBase64() throws IOException, FontFormatException;

    /**
     * 获取验证码
     *
     * @return 验证码
     */
    public abstract String getCaptcha();

    /**
     * 验证码类型
     *
     * @return 验证码类型
     */
    public abstract CaptchaTypeEnum captchaType();

    /**
     * 获取Content-Type
     */
    public abstract ContentTypeEnum contentType();

}
