package io.github.lucky845.basic.captcha.config;

import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * @author created by lucky845 on 2025-01-17
 */
@RequiredArgsConstructor
@Configuration
public class CaptchaGeneratorConfig {

    private final Logger logger = Logger.getLogger(CaptchaGeneratorConfig.class.getName());

    private final List<AbstractCaptchaGeneratorStrategy> strategies;
    private final ThreadPoolTaskExecutor executor;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            strategies.forEach(strategy ->
                    CaptchaGeneratorFactory.registerGenerator(strategy.captchaType(), strategy));
            logger.info("初始化验证码策略完成！");
        }, executor);
    }

}
