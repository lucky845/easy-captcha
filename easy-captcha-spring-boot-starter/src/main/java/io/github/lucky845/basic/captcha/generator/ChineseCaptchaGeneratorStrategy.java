package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.ChineseCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author created by lucky845 on 2025-01-17
 */
@Component
public class ChineseCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private ChineseCaptcha captcha;

    @PostConstruct
    private void init() throws IOException, FontFormatException {
        captcha = new ChineseCaptcha();
        captcha.setFont(captchaProperties.getFont());
        captcha.setCharType(captchaProperties.getCharType());
        captcha.setCaptchaWidth(captchaProperties.getWidth());
        captcha.setCaptchaHeight(captchaProperties.getHeight());
        captcha.setCaptchaLength(captchaProperties.getCaptchaLength());
    }

    @Override
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
