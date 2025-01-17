package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.PngCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 验证码生成类
 *
 * @author created by lucky845 on 2025-01-16
 */
public class PngCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private PngCaptcha captcha;

    /**
     * 生成验证码
     *
     * @param out 输出流
     */
    public void generateCaptcha(OutputStream out) throws IOException, FontFormatException {
        captcha = buildPngCaptcha();
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() throws IOException, FontFormatException {
        captcha = buildPngCaptcha();
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.PNG_CAPTCHA;
    }

    private PngCaptcha buildPngCaptcha() throws IOException, FontFormatException {
        PngCaptcha pngCaptcha = new PngCaptcha();
        pngCaptcha.setFont(captchaProperties.getFont());
        pngCaptcha.setCharType(captchaProperties.getCharType());
        pngCaptcha.setCaptchaWidth(captchaProperties.getWidth());
        pngCaptcha.setCaptchaHeight(captchaProperties.getHeight());
        pngCaptcha.setCaptchaLength(captchaProperties.getCaptchaLength());
        return pngCaptcha;
    }

}
