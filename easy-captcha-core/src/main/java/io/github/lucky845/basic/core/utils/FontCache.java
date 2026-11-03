package io.github.lucky845.basic.core.utils;

import io.github.lucky845.basic.core.enums.FontEnum;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 字体加载缓存。
 *
 * <p>{@link Font#createFont} 每次都会解析整个 TTF 文件，属于重量级操作。
 * 字体对象本身不可变（{@link Font#deriveFont} 返回新实例），可以安全地在多线程间共享，
 * 因此按「字体 + 样式 + 字号」缓存，避免每次生成验证码都重新解析字体。
 *
 * @author created by lucky845 on 2025-01-16
 */
public final class FontCache {

    private static final Logger LOGGER = Logger.getLogger(FontCache.class.getName());

    private static final Map<FontKey, Font> CACHE = new ConcurrentHashMap<>();

    /**
     * 优先使用的中文字体族，按顺序探测。
     */
    private static final String[] CJK_FONT_FAMILIES = {
            "Microsoft YaHei", "微软雅黑", "PingFang SC", "Hiragino Sans GB",
            "Noto Sans CJK SC", "Source Han Sans SC", "WenQuanYi Micro Hei", "SimSun", "宋体"
    };

    private static volatile String cjkFontFamily;

    private FontCache() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 加载内置字体，失败时回退到逻辑字体，保证不会因为字体文件缺失而让整个应用启动失败。
     *
     * @param fontEnum 内置字体
     * @param style    样式，参见 {@link Font#BOLD}
     * @param size     字号
     * @return 字体实例
     */
    public static Font load(FontEnum fontEnum, int style, float size) {
        return CACHE.computeIfAbsent(new FontKey(fontEnum, style, size), FontCache::doLoad);
    }

    /**
     * 获取可渲染中文的字体。
     *
     * @param style 样式
     * @param size  字号
     * @return 字体实例
     */
    public static Font cjkFont(int style, float size) {
        String family = cjkFontFamily();
        try {
            return new Font(family, style, Math.round(size)).deriveFont(size);
        } catch (Exception e) {
            return new Font(Font.SANS_SERIF, style, Math.round(size)).deriveFont(size);
        }
    }

    /**
     * 预热字体缓存，避免首个请求承担字体解析开销。
     */
    public static void warmUp() {
        for (FontEnum fontEnum : FontEnum.values()) {
            load(fontEnum, Font.BOLD, 32f);
        }
        cjkFont(Font.BOLD, 32f);
    }

    private static Font doLoad(FontKey key) {
        String path = key.fontEnum().getPath();
        try (InputStream in = FontCache.class.getResourceAsStream("/" + path)) {
            if (in == null) {
                LOGGER.log(Level.WARNING, () -> "字体文件不存在，回退到逻辑字体: " + path);
                return fallback(key);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(key.style(), key.size());
        } catch (IOException | FontFormatException e) {
            LOGGER.log(Level.WARNING, e, () -> "字体加载失败，回退到逻辑字体: " + path);
            return fallback(key);
        }
    }

    private static Font fallback(FontKey key) {
        return new Font(Font.SANS_SERIF, key.style(), Math.round(key.size())).deriveFont(key.size());
    }

    private static String cjkFontFamily() {
        String cached = cjkFontFamily;
        if (cached != null) {
            return cached;
        }
        synchronized (FontCache.class) {
            if (cjkFontFamily == null) {
                cjkFontFamily = detectCjkFontFamily();
            }
            return cjkFontFamily;
        }
    }

    private static String detectCjkFontFamily() {
        String[] available;
        try {
            available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "无法枚举系统字体，使用逻辑字体", t);
            return Font.SANS_SERIF;
        }
        for (String candidate : CJK_FONT_FAMILIES) {
            for (String family : available) {
                if (family.equalsIgnoreCase(candidate)) {
                    return family;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    private record FontKey(FontEnum fontEnum, int style, float size) {
    }
}
