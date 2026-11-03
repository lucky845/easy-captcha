package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;

import java.awt.*;

/**
 * 中文动态（GIF）验证码生成器，复用 GIF 的逐帧波动渲染。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ChineseGifCaptchaGenerator extends GifCaptchaGenerator {

    private static final double FONT_RATIO = 0.62;

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.CHINESE_GIF_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.GIF;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        String code = generateChineseText(config);
        Font font = cjkFont(config, FONT_RATIO);
        return new CaptchaResult(code, renderAnimated(config, code, font), contentType());
    }
}
