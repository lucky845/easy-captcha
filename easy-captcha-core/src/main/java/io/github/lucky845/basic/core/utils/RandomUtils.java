package io.github.lucky845.basic.core.utils;

import java.security.SecureRandom;

/**
 * 随机数工具类。
 *
 * <p>验证码是安全相关的随机值，必须使用密码学安全的随机源，否则可以被预测、被爆破。
 * {@link SecureRandom} 内部有同步开销，因此每个线程持有独立实例，避免高并发下的锁竞争。
 *
 * @author created by lucky845 on 2025-01-16
 */
public final class RandomUtils {

    private static final ThreadLocal<SecureRandom> SECURE_RANDOM =
            ThreadLocal.withInitial(SecureRandom::new);

    private RandomUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 返回 [0, bound) 区间的随机整数。
     *
     * @param bound 上界（不包含），必须大于 0
     * @return 随机整数
     */
    public static int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound 必须大于 0，当前: " + bound);
        }
        return SECURE_RANDOM.get().nextInt(bound);
    }

    /**
     * 返回 [min, max) 区间的随机整数。
     *
     * @param min 下界（包含）
     * @param max 上界（不包含），必须大于 min
     * @return 随机整数
     */
    public static int nextInt(int min, int max) {
        if (max <= min) {
            throw new IllegalArgumentException(
                    "max 必须大于 min，当前 min=" + min + ", max=" + max);
        }
        return min + SECURE_RANDOM.get().nextInt(max - min);
    }

    /**
     * 返回 [min, max] 闭区间的随机整数（两端都包含）。
     *
     * @param min 下界
     * @param max 上界
     * @return 随机整数
     */
    public static int nextIntInclusive(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException(
                    "max 必须大于等于 min，当前 min=" + min + ", max=" + max);
        }
        return min + SECURE_RANDOM.get().nextInt(max - min + 1);
    }

    /**
     * 从字符数组中随机取一个字符。
     *
     * @param chars 字符数组
     * @return 随机字符
     */
    public static char randomChar(char[] chars) {
        if (chars == null || chars.length == 0) {
            throw new IllegalArgumentException("chars 不能为空");
        }
        return chars[nextInt(chars.length)];
    }

    /**
     * 从数组中等概率取一个元素。
     *
     * @param values 数组
     * @param <T>    元素类型
     * @return 随机元素
     */
    public static <T> T pick(T[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values 不能为空");
        }
        return values[nextInt(values.length)];
    }

    /**
     * 返回 [0, 1) 的随机浮点数。
     */
    public static double nextDouble() {
        return SECURE_RANDOM.get().nextDouble();
    }

    /**
     * 以 {@code trueProbability} 的概率返回 true。
     *
     * @param trueProbability 概率，取值范围 [0, 1]
     */
    public static boolean chance(double trueProbability) {
        if (trueProbability <= 0) {
            return false;
        }
        if (trueProbability >= 1) {
            return true;
        }
        return nextDouble() < trueProbability;
    }
}
