package com.lucky845.basic.captcha.proterties;

import com.lucky845.basic.core.constants.Constants;
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

    private int width;

    private int height;

    private int charType = 0;

    private int captchaType = 0;

    private int captchaLength = 4;

    private int font = 0;


}
