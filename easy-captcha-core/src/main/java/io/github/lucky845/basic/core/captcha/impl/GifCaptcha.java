package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractGifCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;

import java.awt.*;

/**
 * GIF验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class GifCaptcha extends AbstractGifCaptcha {

    @Override
    protected Font font() {
        return getFont();
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.GIF;
    }

    @Override
    public CaptchaTypeEnum getCaptchaType() {
        return CaptchaTypeEnum.GIF_CAPTCHA;
    }
}
