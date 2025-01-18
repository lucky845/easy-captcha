package io.github.lucky845.basic.core.enums;

import io.github.lucky845.basic.core.utils.RandomUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
@Getter
@AllArgsConstructor
public enum ColorEnum {

    RED(new int[]{255, 0, 0}), // 红色
    BLUE(new int[]{0, 0, 255}), // 蓝色
    GREEN(new int[]{0, 255, 0}), // 绿色
    BLACK(new int[]{0, 0, 0}), // 黑色
    GRAY(new int[]{128, 128, 128}), // 灰色
    ORANGE(new int[]{255, 165, 0}), // 橙色
    PURPLE(new int[]{128, 0, 128}), // 紫色
    YELLOW(new int[]{255, 255, 0}), // 黄色
    ;

    /**
     * RGB颜色
     */
    private final int[] colors;

    /**
     * 返回随机颜色
     */
    public static Color getRandomColor() {
        ColorEnum[] values = values();
        ColorEnum colorEnum = values[RandomUtils.num(values.length)];
        return new Color(colorEnum.getColors()[0], colorEnum.getColors()[1], colorEnum.getColors()[2]);
    }

}
