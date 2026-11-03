package io.github.lucky845.basic.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 内置字体枚举。
 *
 * <p>字体文件放在 classpath 的 {@code fonts/} 目录下，路径与
 * {@code easy-captcha-core/src/main/resources/fonts/} 中的实际文件名一一对应。
 *
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@AllArgsConstructor
public enum FontEnum {

    PREFIX(0, "fonts/prefix.ttf"),
    PROGBOT(1, "fonts/progbot.ttf"),
    RANSOM(2, "fonts/ransom.ttf"),
    ROBOT(3, "fonts/robot.ttf"),
    SCANDAL(4, "fonts/scandal.ttf"),
    HEADACHE(5, "fonts/headache.ttf"),
    LEXO(6, "fonts/lexo.ttf"),
    ACTIONJ(7, "fonts/actionj.ttf"),
    EPILOG(8, "fonts/epilog.ttf"),
    FRESNEL(9, "fonts/fresnel.ttf"),
    ;

    /**
     * 字体类型编号，用于配置绑定
     */
    private final int type;

    /**
     * classpath 下的字体路径
     */
    private final String path;

    /**
     * 根据 type 查找枚举实例，未找到返回 {@code null}。
     *
     * @param type 字体类型编号
     * @return 枚举实例或 {@code null}
     */
    public static FontEnum fromType(int type) {
        return Arrays.stream(values())
                .filter(font -> font.type == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 path 查找枚举实例，未找到返回 {@code null}。
     *
     * @param path 字体路径
     * @return 枚举实例或 {@code null}
     */
    public static FontEnum fromPath(String path) {
        return Arrays.stream(values())
                .filter(font -> font.path.equals(path))
                .findFirst()
                .orElse(null);
    }
}
