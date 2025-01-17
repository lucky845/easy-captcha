package com.lucky845.basic.captcha.generator;

import com.lucky845.basic.core.enums.CaptchaTypeEnum;
import com.lucky845.basic.core.exception.CaptchaGeneratorException;
import com.lucky845.basic.core.exception.NoSuchStrategyException;
import org.springframework.beans.MethodInvocationException;

import java.lang.invoke.WrongMethodTypeException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author created by lucky845 on 2025-01-16
 */
public class CaptchaGeneratorFactory {

    private CaptchaGeneratorFactory() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<CaptchaTypeEnum, AbstractCaptchaGeneratorStrategy> CAPTCHA_GENERATOR_MAP = new ConcurrentHashMap<>();

    public static void registerGenerator(CaptchaTypeEnum captchaTypeEnum, AbstractCaptchaGeneratorStrategy generator) {
        CAPTCHA_GENERATOR_MAP.put(captchaTypeEnum, generator);
    }

    public static AbstractCaptchaGeneratorStrategy getGenerator(CaptchaTypeEnum captchaTypeEnum) {
        AbstractCaptchaGeneratorStrategy strategy = CAPTCHA_GENERATOR_MAP.get(captchaTypeEnum);
        return Optional.ofNullable(strategy).orElseThrow(() -> new NoSuchStrategyException("未找到对应的验证码策略，请修改配置后重试！"));
    }

}
