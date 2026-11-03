package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * PNG 字符验证码生成器。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class PngCaptchaGenerator extends AbstractCaptchaGenerator {

    private static final double FONT_RATIO = 0.72;

    private static final double MAX_ROTATION = 15;

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.PNG_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        String code = generateRandomText(config);
        BufferedImage image = newImage(config);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);
            drawInterference(g, config);
            drawDistributedChars(g, code, config, font(config, FONT_RATIO), true, MAX_ROTATION);
        } finally {
            g.dispose();
        }
        return new CaptchaResult(code, encodePng(image), contentType());
    }
}
