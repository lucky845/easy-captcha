package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.ChineseGifCaptcha;
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
public class ChineseGifCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private ChineseGifCaptcha captcha;

    @Override
    public void generateCaptcha(OutputStream out) throws IOException, FontFormatException {
        captcha = buildChineseGifCaptcha();
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() throws IOException, FontFormatException {
        captcha = buildChineseGifCaptcha();
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.CHINESE_GIF_CAPTCHA;
    }

    @Override
    public String contentType() {
        return captcha.getContentType().getContentType();
    }

    private ChineseGifCaptcha buildChineseGifCaptcha() throws IOException, FontFormatException {
        ChineseGifCaptcha chineseGifCaptcha = new ChineseGifCaptcha();
        chineseGifCaptcha.setFont(captchaProperties.getFont());
        chineseGifCaptcha.setCharType(captchaProperties.getCharType());
        chineseGifCaptcha.setCaptchaWidth(captchaProperties.getWidth());
        chineseGifCaptcha.setCaptchaHeight(captchaProperties.getHeight());
        chineseGifCaptcha.setCaptchaLength(captchaProperties.getCaptchaLength());
        return chineseGifCaptcha;
    }
}
