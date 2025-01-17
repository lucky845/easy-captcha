package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.ChineseCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
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

    @Override
    public void generateCaptcha( OutputStream out) throws IOException, FontFormatException {
        captcha = buildChineseCaptcha();
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() throws IOException, FontFormatException {
        captcha = buildChineseCaptcha();
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.CHINESE_CAPTCHA;
    }

    @Override
    public String contentType() {
        return captcha.getContentType().getContentType();
    }

    private ChineseCaptcha buildChineseCaptcha() throws IOException, FontFormatException {
        ChineseCaptcha chineseCaptcha = new ChineseCaptcha();
        chineseCaptcha.setFont(captchaProperties.getFont());
        chineseCaptcha.setCharType(captchaProperties.getCharType());
        chineseCaptcha.setCaptchaWidth(captchaProperties.getWidth());
        chineseCaptcha.setCaptchaHeight(captchaProperties.getHeight());
        chineseCaptcha.setCaptchaLength(captchaProperties.getCaptchaLength());
        return chineseCaptcha;
    }
}
