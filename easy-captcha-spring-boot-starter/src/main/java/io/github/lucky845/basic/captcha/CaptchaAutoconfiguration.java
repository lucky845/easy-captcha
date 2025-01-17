package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author created by lucky845 on 2025-01-16
 */
@ConditionalOnProperty(
        prefix = CaptchaProperties.CAPTCHA_PREFIX,
        value = "enable",
        havingValue = "true",
        matchIfMissing = false
)
@EnableConfigurationProperties(CaptchaProperties.class)
@AutoConfiguration
public class CaptchaAutoconfiguration {

    @Bean
    @ConditionalOnBean(CaptchaGeneratorFactory.class)
    public AbstractCaptchaGeneratorStrategy captchaGeneratorStrategy(CaptchaProperties captchaProperties) {
        return CaptchaGeneratorFactory.getGenerator(CaptchaTypeEnum.getType(captchaProperties.getCaptchaType()));
    }

}
