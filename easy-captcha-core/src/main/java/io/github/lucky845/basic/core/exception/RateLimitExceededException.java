package io.github.lucky845.basic.core.exception;

/**
 * 验证码请求过于频繁。
 *
 * @author created by lucky845
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("请求过于频繁，请稍后再试");
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
