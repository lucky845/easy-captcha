package com.lucky845.basic.core.enums;

import com.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 颜色枚举类，用于定义验证码的常用颜色。
 *
 * @author created by lucky845 on 2025-01-16
 */
public enum ColorEnum {

    RED(new int[]{255, 0, 0}), // 红色
    BLUE(new int[]{0, 0, 255}), // 蓝色
    GREEN(new int[]{0, 255, 0}), // 绿色
    BLACK(new int[]{0, 0, 0}), // 黑色
    GRAY(new int[]{128, 128, 128}), // 灰色
    ORANGE(new int[]{255, 165, 0}), // 橙色
    PURPLE(new int[]{128, 0, 128}), // 紫色
    ;

    ColorEnum(int[] colors) {
        this.colors = colors;
    }

    public int[] getColors() {
        return colors;
    }

    /**
     * RGB颜色
     */
    private final int[] colors;

    private static final Map<int[], ColorEnum> COLOR_MAP;

    static {
        COLOR_MAP = Arrays.stream(ColorEnum.values())
                .collect(Collectors.toMap(ColorEnum::getColors, Function.identity()));
    }

    /**
     * 根据 RGB颜色 查找对应的枚举实例
     *
     * @param colors RGB颜色
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static ColorEnum fromType(int[] colors) {
        return COLOR_MAP.get(colors);
    }

    /**
     * 返回随机颜色
     */
    public static Color getRandomColor() {
        ColorEnum[] values = values();
        ColorEnum colorEnum = values[RandomUtils.num(values.length)];
        return new Color(colorEnum.getColors()[0], colorEnum.getColors()[1], colorEnum.getColors()[2]);
    }

}
