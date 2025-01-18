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
public enum CharTypeEnum {

    /**
     * 默认，随机
     */
    DEFAULT(0),

    /**
     * 纯数字
     */
    PURE_NUMBER(1),

    /**
     * 纯字母
     */
    PURE_LETTERS(2),

    /**
     * 纯大写字母
     */
    PURE_CAPITAL_LETTERS(3),

    /**
     * 纯小写字母
     */
    PURE_LOWERCASE_LETTERS(4),

    /**
     * 字母数字混合
     */
    ALPHANUMERIC_MIX(5),

    ;

    private final int type;

    private static final Map<Integer, CharTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(e -> e.type, Function.identity()));
    }

    public static CharTypeEnum fromType(int type) {
        return CACHE.get(type);
    }

}
