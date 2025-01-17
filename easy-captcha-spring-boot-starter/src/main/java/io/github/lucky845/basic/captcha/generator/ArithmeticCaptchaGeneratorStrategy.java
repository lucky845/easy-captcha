package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.impl.ArithmeticCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author created by lucky845 on 2025-01-17
 */
public class ArithmeticCaptchaGeneratorStrategy extends AbstractCaptchaGeneratorStrategy {

    private ArithmeticCaptcha captcha;

    @Override
    public void generateCaptcha(OutputStream out) throws IOException, FontFormatException {
        captcha = buildArithmeticCaptcha();
        boolean success = captcha.createCaptcha(out);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
    }

    @Override
    public String generateCaptchaBase64() throws IOException, FontFormatException {
        captcha = buildArithmeticCaptcha();
        return captcha.createBase64Captcha();
    }

    @Override
    public String getCaptcha() {
        return captcha.getCaptcha();
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.ARITHMETIC_CAPTCHA;
    }

    private ArithmeticCaptcha buildArithmeticCaptcha() throws IOException, FontFormatException {
        ArithmeticCaptcha arithmeticCaptcha = new ArithmeticCaptcha();
        arithmeticCaptcha.setFont(captchaProperties.getFont());
        arithmeticCaptcha.setCharType(captchaProperties.getCharType());
        arithmeticCaptcha.setCaptchaWidth(captchaProperties.getWidth());
        arithmeticCaptcha.setCaptchaHeight(captchaProperties.getHeight());
        arithmeticCaptcha.setCaptchaLength(captchaProperties.getCaptchaLength());
        return arithmeticCaptcha;
    }

}
