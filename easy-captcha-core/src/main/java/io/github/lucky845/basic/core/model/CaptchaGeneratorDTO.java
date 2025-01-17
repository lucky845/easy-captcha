package io.github.lucky845.basic.core.model;

import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;

/**
 * @author created by lucky845 on 2025-01-16
 */
public class CaptchaGeneratorDTO {

    public CaptchaGeneratorDTO() {
    }

    public CaptchaGeneratorDTO(CharTypeEnum charType, CaptchaTypeEnum captchaType, FontEnum font, int captchaLength, int captchaWidth, int captchaHeight) {
        this.charType = charType;
        this.captchaType = captchaType;
        this.font = font;
        this.captchaLength = captchaLength;
        this.captchaWidth = captchaWidth;
        this.captchaHeight = captchaHeight;
    }


    /**
     * 验证码字符类型
     */
    private CharTypeEnum charType;

    /**
     * 验证码类型
     */
    private CaptchaTypeEnum captchaType;

    /**
     * 字体
     */
    private FontEnum font;

    /**
     * 验证码长度
     */
    private int captchaLength;

    /**
     * 验证码显示宽度
     */
    private int captchaWidth;

    /**
     * 验证码显示高度
     */
    private int captchaHeight;

    public CharTypeEnum getCharType() {
        return charType;
    }

    public void setCharType(CharTypeEnum charType) {
        this.charType = charType;
    }

    public CaptchaTypeEnum getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(CaptchaTypeEnum captchaType) {
        this.captchaType = captchaType;
    }

    public FontEnum getFont() {
        return font;
    }

    public void setFont(FontEnum font) {
        this.font = font;
    }

    public int getCaptchaLength() {
        return captchaLength;
    }

    public void setCaptchaLength(int captchaLength) {
        this.captchaLength = captchaLength;
    }

    public int getCaptchaWidth() {
        return captchaWidth;
    }

    public void setCaptchaWidth(int captchaWidth) {
        this.captchaWidth = captchaWidth;
    }

    public int getCaptchaHeight() {
        return captchaHeight;
    }

    public void setCaptchaHeight(int captchaHeight) {
        this.captchaHeight = captchaHeight;
    }

}
