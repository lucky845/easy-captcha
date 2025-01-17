package com.lucky845.basic.core.model;

import com.lucky845.basic.core.enums.CaptchaTypeEnum;
import com.lucky845.basic.core.enums.CharTypeEnum;
import com.lucky845.basic.core.enums.FontEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author created by lucky845 on 2025-01-16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CaptchaGeneratorDTO {

    /**
     * 验证码字符类型
     */
    private CharTypeEnum charType;

    /**
     * 验证码类型
     */
    private CaptchaTypeEnum captchaType;

    /**
     * 字体
     */
    private FontEnum font;

    /**
     * 验证码长度
     */
    private int captchaLength;

    /**
     * 验证码显示宽度
     */
    private int captchaWidth;

    /**
     * 验证码显示高度
     */
    private int captchaHeight;

}
