package com.lucky845.basic.core.exception;

/**
 * 验证码生成失败异常
 *
 * @author created by lucky845 on 2025-01-16
 */
public class CaptchaGeneratorException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "生成算数验证码失败，请稍后重试!";

    public CaptchaGeneratorException() {
        super(DEFAULT_MESSAGE);
    }

    public CaptchaGeneratorException(String message) {
        super(message);
    }

    public CaptchaGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

}
