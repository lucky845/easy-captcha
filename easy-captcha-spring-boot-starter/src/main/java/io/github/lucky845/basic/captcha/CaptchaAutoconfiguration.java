package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.config.CaptchaGeneratorConfig;
import io.github.lucky845.basic.captcha.config.ThreadPoolConfig;
import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @author created by lucky845 on 2025-01-16
 */
@ComponentScan("io.github.lucky845.basic.captcha")
@EnableConfigurationProperties(CaptchaProperties.class)
@AutoConfiguration
@Import({CaptchaGeneratorConfig.class, ThreadPoolConfig.class})
public class CaptchaAutoconfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = CaptchaProperties.CAPTCHA_PREFIX,
            name = "enable",
            havingValue = "true",
            matchIfMissing = true
    )
    public CaptchaService captchaService(CaptchaProperties properties) {
        return new CaptchaService(properties);
    }

}
