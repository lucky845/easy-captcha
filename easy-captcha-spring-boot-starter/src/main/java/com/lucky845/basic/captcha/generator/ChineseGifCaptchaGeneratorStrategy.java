package com.lucky845.basic.captcha.generator;

import com.lucky845.basic.core.captcha.impl.ChineseGifCaptcha;
import com.lucky845.basic.core.enums.CaptchaTypeEnum;
import com.lucky845.basic.core.exception.CaptchaGeneratorException;
import com.lucky845.basic.core.model.CaptchaGeneratorDTO;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author created by lucky845 on 2025-01-17
 */
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
