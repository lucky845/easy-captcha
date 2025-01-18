package io.github.lucky845.basic.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 难度级别枚举
 *
 * @author created by lucky845 on 2025-01-18
 */
@Getter
@AllArgsConstructor
public enum ArithmeticDifficultyEnum {

    EASY(1),      // 简单: 个位数运算
    MEDIUM(2),    // 中等: 两位数运算
    HARD(3),      // 困难: 多运算符组合
    EXPERT(4),    // 专家: 包含括号的复杂运算
    ;

    private static final Map<Integer, ArithmeticDifficultyEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(ArithmeticDifficultyEnum::getLevel, Function.identity()));
    }

    private final int level;

    /**
     * 获取难度级别
     */
    public static ArithmeticDifficultyEnum fromLevel(int level) {
        return CACHE.getOrDefault(level, EASY);
    }

}
