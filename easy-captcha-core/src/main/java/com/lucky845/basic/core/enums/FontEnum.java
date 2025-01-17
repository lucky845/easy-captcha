package com.lucky845.basic.core.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 字体枚举类
 *
 * @author created by lucky845 on 2025-01-16
 */
public enum FontEnum {

    PREFIX(0, "fonts/prefix.ttf"),
    PROGBOT(1, "fonts/progbot.ttf"),
    RANSOM(2, "fonts/ransom.ttf"),
    ROBOT(3, "fonts/roboto.ttf"),
    SCANDAL(4, "fonts/scandal.ttf"),
    HEADACHE(5, "fonts/headache.ttf"),
    IEXO(6, "fonts/iexo.ttf"),
    ACTIONJ(7, "fonts/actionj.ttf"),
    EPLIOG(8, "fonts/epliog.ttf"),
    FRESNEL(9, "fonts/fresnel.ttf"),
    ;

    FontEnum(int type, String path) {
        this.type = type;
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    // 字体类型
    private final int type;
    // 字体路径
    private final String path;

    // 根据 type 查找枚举实例的 Map
    private static final Map<Integer, FontEnum> FONT_ENUM_MAP;

    static {
        // 初始化 FONT_ENUM_MAP
        FONT_ENUM_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(FontEnum::getType, Function.identity()));
    }

    /**
     * 根据 type 查找对应的枚举实例
     *
     * @param type 字体类型
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static FontEnum fromType(int type) {
        return FONT_ENUM_MAP.get(type);
    }

    /**
     * 根据 path 查找对应的枚举实例
     *
     * @param path 字体路径
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static FontEnum fromPath(String path) {
        return Arrays.stream(values())
                .filter(font -> font.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }
}
