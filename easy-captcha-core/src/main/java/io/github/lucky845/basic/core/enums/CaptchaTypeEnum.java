package io.github.lucky845.basic.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@AllArgsConstructor
public enum CaptchaTypeEnum {

    /**
     * png验证码
     */
    PNG_CAPTCHA(0),

    /**
     * GIF验证码
     */
    GIF_CAPTCHA(1),

    /**
     * 中文png验证码
     */
    CHINESE_CAPTCHA(2),

    /**
     * 中文gif验证码
     */
    CHINESE_GIF_CAPTCHA(3),

    /**
     * 算术验证码
     */
    ARITHMETIC_CAPTCHA(4),

    ;

    private static final Map<Integer, CaptchaTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(CaptchaTypeEnum::getType, Function.identity()));
    }

    private final int type;

    /**
     * 根据type获取
     *
     * @param type type
     */
    public static CaptchaTypeEnum fromType(int type) {
        return CACHE.get(type);
    }

}
