package io.github.lucky845.basic.captcha.behavior;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的滑动窗口限流器。
 *
 * <p>验证码接口是最容易被刷的入口之一：攻击者可以疯狂请求图片做打码训练，
 * 也可以对签名接口暴力试错。按 key（通常是 IP 或用户 id）限制单位时间内的次数，
 * 是成本最低的第一道防线。
 *
 * <p>实现使用固定窗口：每个窗口内计数，窗口过期后重新计数。固定窗口在窗口边界
 * 会有约 2 倍的瞬时突刺，但对验证码这种场景足够用，且内存占用可控。
 *
 * @author created by lucky845
 */
public class CaptchaRateLimiter {

    /**
     * key → 当前窗口计数
     */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private final int limit;

    private final long windowMillis;

    /**
     * @param limit        窗口内允许的最大次数
     * @param windowMillis 窗口长度（毫秒）
     */
    public CaptchaRateLimiter(int limit, long windowMillis) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit 必须大于 0，当前值：" + limit);
        }
        if (windowMillis < 1) {
            throw new IllegalArgumentException("windowMillis 必须大于 0，当前值：" + windowMillis);
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    /**
     * 尝试获取一次配额。
     *
     * @param key 限流维度，例如 IP
     * @return {@code true} 表示允许本次请求
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            // 拿不到 key 时不限制，避免因为取不到 IP 而误伤全部用户
            return true;
        }
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMillis) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        // 顺手清理过期窗口，防止 map 无限增长
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= windowMillis);
        }
        return window.count.get() <= limit;
    }

    /**
     * 当前还能用几次。
     *
     * @param key 限流维度
     */
    public int remaining(String key) {
        if (key == null || key.isBlank()) {
            return limit;
        }
        Window window = windows.get(key);
        if (window == null || System.currentTimeMillis() - window.start >= windowMillis) {
            return limit;
        }
        return Math.max(0, limit - window.count.get());
    }

    /**
     * 清空所有计数。
     */
    public void reset() {
        windows.clear();
    }

    /**
     * 单个窗口
     *
     * @param start 窗口开始时间
     * @param count 计数
     */
    private record Window(long start, AtomicInteger count) {
    }
}