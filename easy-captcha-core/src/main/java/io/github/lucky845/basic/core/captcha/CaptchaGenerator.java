package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;

/**
 * 验证码生成器。
 *
 * <p>实现必须是无状态的：所有生成所需的参数都从 {@link CaptchaConfig} 传入，
 * 每次 {@link #generate(CaptchaConfig)} 都产生一份全新的 {@link CaptchaResult}。
 * 这样同一个生成器实例可以被任意数量的线程并发调用而不需要加锁。
 *
 * @author created by lucky845 on 2025-01-16
 */
public interface CaptchaGenerator {

    /**
     * 验证码类型。
     */
    CaptchaTypeEnum captchaType();

    /**
     * 输出图片的内容类型。
     */
    ContentTypeEnum contentType();

    /**
     * 生成一份新的验证码。
     *
     * @param config 生成配置
     * @return 验证码结果，包含答案与图片
     */
    CaptchaResult generate(CaptchaConfig config);
}
