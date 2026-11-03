package io.github.lucky845.basic.captcha.generator;

import io.github.lucky845.basic.core.captcha.CaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ArithmeticCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ChineseCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ChineseGifCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ClickWordCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.GifCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.PngCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.SliderCaptchaGenerator;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.exception.NoSuchStrategyException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 验证码生成器注册表。
 *
 * <p>构造时一次性注册全部内置生成器并合入调用方提供的自定义生成器，
 * 之后注册表只读，因此天然线程安全，也不存在"异步注册完成前请求先到"的竞态。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class CaptchaGeneratorRegistry {

    private final Map<CaptchaTypeEnum, CaptchaGenerator> generators;

    /**
     * @param customGenerators 自定义生成器，同类型会覆盖内置实现，可为 null
     */
    public CaptchaGeneratorRegistry(List<CaptchaGenerator> customGenerators) {
        EnumMap<CaptchaTypeEnum, CaptchaGenerator> map = new EnumMap<>(CaptchaTypeEnum.class);
        registerDefaults(map);
        if (customGenerators != null) {
            for (CaptchaGenerator generator : customGenerators) {
                if (generator == null || generator.captchaType() == null) {
                    throw new IllegalStateException("自定义验证码生成器必须声明 captchaType()");
                }
                map.put(generator.captchaType(), generator);
            }
        }
        this.generators = Collections.unmodifiableMap(map);
    }

    private static void registerDefaults(EnumMap<CaptchaTypeEnum, CaptchaGenerator> map) {
        put(map, new PngCaptchaGenerator());
        put(map, new GifCaptchaGenerator());
        put(map, new ChineseCaptchaGenerator());
        put(map, new ChineseGifCaptchaGenerator());
        put(map, new ArithmeticCaptchaGenerator());
        put(map, new SliderCaptchaGenerator());
        put(map, new ClickWordCaptchaGenerator());
    }

    private static void put(EnumMap<CaptchaTypeEnum, CaptchaGenerator> map, CaptchaGenerator generator) {
        map.put(generator.captchaType(), generator);
    }

    /**
     * 按类型取出生成器。
     *
     * @param type 验证码类型
     * @return 对应生成器
     * @throws NoSuchStrategyException 类型为空或未注册时抛出
     */
    public CaptchaGenerator get(CaptchaTypeEnum type) {
        if (type == null) {
            throw new NoSuchStrategyException("验证码类型不能为空");
        }
        CaptchaGenerator generator = generators.get(type);
        if (generator == null) {
            throw new NoSuchStrategyException("未注册的验证码类型：" + type);
        }
        return generator;
    }

    /**
     * 已注册的全部类型。
     */
    public Set<CaptchaTypeEnum> types() {
        return generators.keySet();
    }
}
