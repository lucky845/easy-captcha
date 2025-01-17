package com.lucky845.basic.captcha.proterties;

import com.lucky845.basic.core.constants.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author created by lucky845 on 2025-01-16
 */
@ConfigurationProperties(prefix = CaptchaProperties.CAPTCHA_PREFIX)
public class CaptchaProperties {

    public static final String CAPTCHA_PREFIX = Constants.PROJECT_BASE_PREFIX;

    private boolean enabled = true;

    private int width;

    private int height;

    private int charType = 0;

    private int captchaType = 0;

    private int captchaLength = 4;

    private int font = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCharType() {
        return charType;
    }

    public void setCharType(int charType) {
        this.charType = charType;
    }

    public int getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(int captchaType) {
        this.captchaType = captchaType;
    }

    public int getCaptchaLength() {
        return captchaLength;
    }

    public void setCaptchaLength(int captchaLength) {
        this.captchaLength = captchaLength;
    }

    public int getFont() {
        return font;
    }

    public void setFont(int font) {
        this.font = font;
    }
}
