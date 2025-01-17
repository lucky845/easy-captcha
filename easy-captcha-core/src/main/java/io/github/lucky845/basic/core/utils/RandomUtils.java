package io.github.lucky845.basic.core.utils;

import io.github.lucky845.basic.core.constants.Constants;

import java.security.SecureRandom;

/**
 * @author created by lucky845 on 2025-01-16
 */
public class RandomUtils {

    private RandomUtils() {
        throw new AssertionError();
    }

    private static final SecureRandom SECURE_RANDOM;

    static {
        SECURE_RANDOM = new SecureRandom();
        SECURE_RANDOM.setSeed(System.currentTimeMillis());
    }

    /**
     * 产生两个数之间的随机数
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    public static int num(int min, int max) {
        return SECURE_RANDOM.nextInt(max - min) + min;
    }

    /**
     * 产生0-max之间的随机数
     *
     * @param max 最大值
     */
    public static int num(int max) {
        return SECURE_RANDOM.nextInt(max);
    }

    /**
     * 返回CHARS中的随机字符
     *
     * @return 随机字符
     */
    public static char chars() {
        return Constants.CHARS[num(Constants.CHARS.length)];
    }

    /**
     * 返回CHARS中第0位到第num位的随机字符
     *
     * @param num 到第几位结束
     * @return 随机字符
     */
    public static char chars(int num) {
        return Constants.CHARS[num(num)];
    }

    /**
     * 返回CHARS中第min位到第max位的随机字符
     *
     * @param min 从第几位开始
     * @param max 到第几位结束
     * @return 随机字符
     */
    public static char chars(int min, int max) {
        return Constants.CHARS[num(min, max)];
    }

}
