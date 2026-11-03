package io.github.lucky845.basic.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 验证码类型。
 *
 * <p>按交互形态分两族：
 * <ul>
 *     <li><b>静态图片</b>：答案是文字或算式，用户直接输入；</li>
 *     <li><b>行为验证码</b>：答案是坐标/角度等几何信息，由前端把用户操作产生的
 *         轨迹回传，后端按容差判定，无法仅靠"看图打码"破解。</li>
 * </ul>
 *
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@AllArgsConstructor
public enum CaptchaTypeEnum {

    /**
     * png验证码
     */
    PNG_CAPTCHA(0, Kind.TEXT),

    /**
     * GIF验证码
     */
    GIF_CAPTCHA(1, Kind.TEXT),

    /**
     * 中文png验证码
     */
    CHINESE_CAPTCHA(2, Kind.TEXT),

    /**
     * 中文gif验证码
     */
    CHINESE_GIF_CAPTCHA(3, Kind.TEXT),

    /**
     * 算术验证码
     */
    ARITHMETIC_CAPTCHA(4, Kind.TEXT),

    /**
     * 滑块拼图验证码：拖动滑块把缺口补齐
     */
    SLIDER_CAPTCHA(5, Kind.BEHAVIOR),

    /**
     * 文字点选验证码：按提示顺序点击图上的汉字
     */
    CLICK_WORD_CAPTCHA(6, Kind.BEHAVIOR),

    ;

    private static final Map<Integer, CaptchaTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(CaptchaTypeEnum::getType, Function.identity()));
    }

    private final int type;

    /**
     * 交互形态
     */
    private final Kind kind;

    /**
     * 根据type获取
     *
     * @param type type
     */
    public static CaptchaTypeEnum fromType(int type) {
        return CACHE.get(type);
    }

    /**
     * 是否行为验证码
     */
    public boolean isBehavior() {
        return kind == Kind.BEHAVIOR;
    }

    /**
     * 验证码交互形态
     */
    public enum Kind {
        /**
         * 用户输入文字答案
         */
        TEXT,
        /**
         * 用户完成拖动/点击等动作，由后端按容差判定
         */
        BEHAVIOR
    }
}
