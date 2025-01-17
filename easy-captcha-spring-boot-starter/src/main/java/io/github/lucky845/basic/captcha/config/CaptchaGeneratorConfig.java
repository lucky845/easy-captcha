package io.github.lucky845.basic.captcha.config;

import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author created by lucky845 on 2025-01-17
 */
@Configuration
public class CaptchaGeneratorConfig {

    private List<AbstractCaptchaGeneratorStrategy> strategies;

    public CaptchaGeneratorConfig(List<AbstractCaptchaGeneratorStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        strategies.forEach(strategy ->
                CaptchaGeneratorFactory.registerGenerator(strategy.captchaType(), strategy));
    }

}
