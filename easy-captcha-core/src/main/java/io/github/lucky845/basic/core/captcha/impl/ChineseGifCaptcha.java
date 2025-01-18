package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractGifCaptcha;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;

import java.awt.*;
import java.util.Random;

/**
 * 中文 GIF 验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ChineseGifCaptcha extends AbstractGifCaptcha {

    @Override
    protected Font font() {
        return new Font("宋体", Font.BOLD, 32);
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.GIF;
    }

    @Override
    public CaptchaTypeEnum getCaptchaType() {
        return CaptchaTypeEnum.CHINESE_GIF_CAPTCHA;
    }
}
