package io.github.lucky845.basic.core.exception;

/**
 * 未找到对应的验证码策略异常
 *
 * @author created by lucky845 on 2025-01-16
 */
public class NoSuchStrategyException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "未找到对应的验证码策略，请修改配置后重试！";

    public NoSuchStrategyException() {
        super(DEFAULT_MESSAGE);
    }

    public NoSuchStrategyException(String message) {
        super(message);
    }

    public NoSuchStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

}
