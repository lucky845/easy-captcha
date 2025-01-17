package com.lucky845.basic.captcha.generator;

import com.lucky845.basic.core.captcha.impl.GifCaptcha;
import com.lucky845.basic.core.enums.CaptchaTypeEnum;
import com.lucky845.basic.core.exception.CaptchaGeneratorException;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author created by lucky845 on 2025-01-17
 */
public class GifCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private GifCaptcha captcha;

    @Override
    public void generateCaptcha(OutputStream out) throws IOException, FontFormatException {
        captcha = buildGifCaptcha();
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() throws IOException, FontFormatException {
        captcha = buildGifCaptcha();
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.GIF_CAPTCHA;
    }

    private GifCaptcha buildGifCaptcha() throws IOException, FontFormatException {
        GifCaptcha gifCaptcha = new GifCaptcha();
        gifCaptcha.setFont(captchaProperties.getFont());
        gifCaptcha.setCharType(captchaProperties.getCharType());
        gifCaptcha.setCaptchaWidth(captchaProperties.getWidth());
        gifCaptcha.setCaptchaHeight(captchaProperties.getHeight());
        gifCaptcha.setCaptchaLength(captchaProperties.getCaptchaLength());
        return gifCaptcha;
    }

}
