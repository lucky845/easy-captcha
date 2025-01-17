package io.github.lucky845.basic.captcha.config;

import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author created by lucky845 on 2025-01-17
 */
@Configuration
public class CaptchaGeneratorConfig {

    private final Logger logger = Logger.getLogger(CaptchaGeneratorConfig.class.getName());

    private final List<AbstractCaptchaGeneratorStrategy> strategies;

    public CaptchaGeneratorConfig(List<AbstractCaptchaGeneratorStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        strategies.forEach(strategy ->
                CaptchaGeneratorFactory.registerGenerator(strategy.captchaType(), strategy));
        logger.info("初始化验证码策略完成！");
    }

}
