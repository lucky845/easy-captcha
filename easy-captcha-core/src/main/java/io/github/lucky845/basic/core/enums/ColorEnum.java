package io.github.lucky845.basic.core.enums;

import io.github.lucky845.basic.core.utils.RandomUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

/**
 * 验证码常用颜色枚举。
 *
 * <p>只保留在白底上可读的深色，避免出现黄色、亮绿色这类对比度过低、
 * 用户肉眼难以辨认的颜色。
 *
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@AllArgsConstructor
public enum ColorEnum {

    RED(200, 0, 0),
    BLUE(0, 0, 200),
    GREEN(0, 140, 0),
    BLACK(0, 0, 0),
    DARK_GRAY(70, 70, 70),
    ORANGE(200, 110, 0),
    PURPLE(110, 0, 140),
    TEAL(0, 110, 110),
    ;

    private final int red;
    private final int green;
    private final int blue;

    /**
     * 返回一个随机的深色。
     *
     * @return 随机颜色
     */
    public static Color randomColor() {
        ColorEnum colorEnum = RandomUtils.pick(values());
        return new Color(colorEnum.red, colorEnum.green, colorEnum.blue);
    }

    /**
     * 返回一个随机的浅色，用于干扰元素，保证不会盖住字符。
     *
     * @return 随机浅色
     */
    public static Color randomLightColor() {
        int r = RandomUtils.nextIntInclusive(140, 230);
        int g = RandomUtils.nextIntInclusive(140, 230);
        int b = RandomUtils.nextIntInclusive(140, 230);
        return new Color(r, g, b);
    }
}
