package io.github.lucky845.basic.captcha;


import io.github.lucky845.basic.captcha.generator.AbstractCaptchaGeneratorStrategy;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorFactory;
import io.github.lucky845.basic.captcha.proterties.CaptchaProperties;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.io.IOException;

/**
 * @author created by lucky845 on 2025-01-17
 */
@RequiredArgsConstructor
public class CaptchaService {

    /**
     * 获取验证码
     */
    @Getter
    private String captcha;

    private final CaptchaProperties captchaProperties;

    /**
     * 检验验证码是否生成
     *
     * @return 是否生成
     */
    public boolean isCaptchaGenerated() {
        return captcha != null;
    }

    /**
     * 生成验证码
     *
     * @param response 响应体
     */
    public void generateCaptcha(HttpServletResponse response) {
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            AbstractCaptchaGeneratorStrategy strategy = CaptchaGeneratorFactory.getGenerator(captchaProperties.getCaptchaType());
            // 设置响应头
            response.setContentType(strategy.contentType().getContentType());
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            strategy.generateCaptcha(outputStream);
            // 保存验证码
            captcha = strategy.getCaptcha();
            outputStream.flush();
        } catch (IOException | FontFormatException e) {
            throw new CaptchaGeneratorException();
        }
    }

    /**
     * 生成Base64验证码
     *
     * @return Base64验证码
     */
    public String generateCaptchaBase64() {
        try {
            AbstractCaptchaGeneratorStrategy strategy = CaptchaGeneratorFactory.getGenerator(captchaProperties.getCaptchaType());
            String base64Captcha = strategy.generateCaptchaBase64();
            // 保存验证码
            captcha = strategy.getCaptcha();
            return base64Captcha;
        } catch (IOException | FontFormatException e) {
            throw new CaptchaGeneratorException();
        }
    }

}
