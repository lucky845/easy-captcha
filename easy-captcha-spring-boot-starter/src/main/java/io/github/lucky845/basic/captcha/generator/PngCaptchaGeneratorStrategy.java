package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.PngCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 验证码生成类
 *
 * @author created by lucky845 on 2025-01-16
 */
@Component
public class PngCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private PngCaptcha captcha;

    @PostConstruct
    private void init() throws IOException, FontFormatException {
        captcha = new PngCaptcha();
        captcha.setFont(captchaProperties.getFont());
        captcha.setCharType(captchaProperties.getCharType());
        captcha.setCaptchaWidth(captchaProperties.getWidth());
        captcha.setCaptchaHeight(captchaProperties.getHeight());
        captcha.setCaptchaLength(captchaProperties.getCaptchaLength());
    }

    /**
     * 生成验证码
     *
     * @param out 输出流
     */
    public void generateCaptcha(OutputStream out) {
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() {
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return captcha.getCaptchaType();
    }

    @Override
    public ContentTypeEnum contentType() {
        return captcha.getContentType();
    }

}
