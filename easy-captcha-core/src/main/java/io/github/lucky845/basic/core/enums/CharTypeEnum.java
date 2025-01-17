package io.github.lucky845.basic.core.enums;


import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author created by lucky845 on 2025-01-16
 */
public enum CharTypeEnum {

    /**
     * 默认，随机
     */
    DEFAULT(0),

    /**
     * 纯数字
     */
    TYPE_ONLY_NUMBER(1),

    /**
     * 纯字母
     */
    TYPE_ONLY_CAPTCHA(2),

    /**
     * 纯大写字母
     */
    TYPE_ONLY_CAPTCHA_NUM(3),

    /**
     * 纯小写字母
     */
    TYPE_ONLY_CAPTCHA_NUM_NUM(4),

    /**
     * 字母数字混合
     */
    TYPE_ONLY_CAPTCHA_NUM_NUM_NUM(5),

    ;

    CharTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    private final int type;

    private static final Map<Integer, CharTypeEnum> TYPE_ENUM_MAP;

    static {
        TYPE_ENUM_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(e -> e.type, Function.identity()));
    }

    public static CharTypeEnum fromType(int type) {
        return TYPE_ENUM_MAP.get(type);
    }

}
