package io.github.lucky845.basic.captcha.proterties;

import io.github.lucky845.basic.core.constants.Constants;
import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author created by lucky845 on 2025-01-16
 */
@Data
@ConfigurationProperties(prefix = CaptchaProperties.CAPTCHA_PREFIX)
public class CaptchaProperties {

    public static final String CAPTCHA_PREFIX = Constants.PROJECT_BASE_PREFIX;

    private boolean enabled = true;

    private int width = 200;

    private int height = 50;

    private CharTypeEnum charType = CharTypeEnum.DEFAULT;

    private CaptchaTypeEnum captchaType = CaptchaTypeEnum.PNG_CAPTCHA;

    private int captchaLength = 4;

    private FontEnum font = FontEnum.ACTIONJ;

    private ArithmeticDifficultyEnum difficulty = ArithmeticDifficultyEnum.EASY;

}
