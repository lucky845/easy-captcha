package io.github.lucky845.basic.core.config;

import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;

import java.util.Objects;

/**
 * 验证码生成配置，不可变对象。
 *
 * <p>配置在构造时完成校验，非法值立即抛出 {@link IllegalArgumentException}，
 * 避免运行到画图阶段才因为宽高为 0、随机区间倒挂之类的错误崩溃。
 *
 * @param width      图片宽度（像素）
 * @param height     图片高度（像素）
 * @param length     验证码字符个数
 * @param charType   字符类型
 * @param font       字体
 * @param fontSize   基础字号，实际绘制时会按图片高度自适应收缩
 * @param difficulty 算术验证码难度
 * @author created by lucky845 on 2025-01-16
 */
public record CaptchaConfig(
        int width,
        int height,
        int length,
        CharTypeEnum charType,
        FontEnum font,
        float fontSize,
        ArithmeticDifficultyEnum difficulty
) {

    public static final int DEFAULT_WIDTH = 200;
    public static final int DEFAULT_HEIGHT = 50;
    public static final int DEFAULT_LENGTH = 4;
    public static final float DEFAULT_FONT_SIZE = 32f;

    public static final int MIN_WIDTH = 60;
    public static final int MAX_WIDTH = 2000;
    public static final int MIN_HEIGHT = 30;
    public static final int MAX_HEIGHT = 1000;
    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 10;

    public CaptchaConfig {
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException(
                    "width 必须在 [" + MIN_WIDTH + ", " + MAX_WIDTH + "] 之间，当前: " + width);
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            throw new IllegalArgumentException(
                    "height 必须在 [" + MIN_HEIGHT + ", " + MAX_HEIGHT + "] 之间，当前: " + height);
        }
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "length 必须在 [" + MIN_LENGTH + ", " + MAX_LENGTH + "] 之间，当前: " + length);
        }
        if (fontSize < 10f || fontSize > 200f) {
            throw new IllegalArgumentException("fontSize 必须在 [10, 200] 之间，当前: " + fontSize);
        }
        Objects.requireNonNull(charType, "charType 不能为空");
        Objects.requireNonNull(font, "font 不能为空");
        Objects.requireNonNull(difficulty, "difficulty 不能为空");
    }

    /**
     * 默认配置：200x50、4 位字符、默认字符集、actionj 字体、简单算术。
     */
    public static CaptchaConfig defaults() {
        return new CaptchaConfig(
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
                DEFAULT_LENGTH,
                CharTypeEnum.DEFAULT,
                FontEnum.ACTIONJ,
                DEFAULT_FONT_SIZE,
                ArithmeticDifficultyEnum.EASY
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 根据图片高度计算实际字号，保证字符不会超出画布。
     *
     * @param ratio 字号占高度的比例
     * @return 实际字号
     */
    public float effectiveFontSize(double ratio) {
        float byHeight = (float) (height * ratio);
        return Math.max(10f, Math.min(fontSize, byHeight));
    }

    public static final class Builder {
        private int width = DEFAULT_WIDTH;
        private int height = DEFAULT_HEIGHT;
        private int length = DEFAULT_LENGTH;
        private CharTypeEnum charType = CharTypeEnum.DEFAULT;
        private FontEnum font = FontEnum.ACTIONJ;
        private float fontSize = DEFAULT_FONT_SIZE;
        private ArithmeticDifficultyEnum difficulty = ArithmeticDifficultyEnum.EASY;

        private Builder() {
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder length(int length) {
            this.length = length;
            return this;
        }

        public Builder charType(CharTypeEnum charType) {
            this.charType = charType;
            return this;
        }

        public Builder font(FontEnum font) {
            this.font = font;
            return this;
        }

        public Builder fontSize(float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder difficulty(ArithmeticDifficultyEnum difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public CaptchaConfig build() {
            return new CaptchaConfig(width, height, length, charType, font, fontSize, difficulty);
        }
    }
}
