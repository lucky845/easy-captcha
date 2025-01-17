package io.github.lucky845.basic.core.captcha;


import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import java.io.OutputStream;

/**
 * 验证码接口
 *
 * @author created by lucky845 on 2025-01-16
 */
public interface Captcha {

    /**
     * 生成验证码
     *
     * @param out 输出流
     * @return 是否成功
     */
    boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException;

    /**
     * 生成Base64验证码
     *
     * @return Base64的验证码
     */
    String createBase64Captcha() throws Exception;

    /**
     * 获取图片类型
     *
     * @return 图片格式，MIME类型
     */
    ContentTypeEnum getContentType();


}
