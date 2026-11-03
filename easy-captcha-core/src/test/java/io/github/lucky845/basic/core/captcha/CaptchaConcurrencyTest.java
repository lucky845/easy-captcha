package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.captcha.impl.ArithmeticCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.GifCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.PngCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发安全测试。
 *
 * <p>回归历史 bug：旧实现让所有线程共享同一个 {@code captcha} 实例字段，
 * 并发时验证码会互相覆盖，甚至所有用户拿到同一个答案。
 *
 * @author created by lucky845
 */
@DisplayName("生成器并发安全")
class CaptchaConcurrencyTest {

    private static final CaptchaConfig CONFIG = CaptchaConfig.defaults();

    private static final int THREADS = 32;

    private static final int PER_THREAD = 50;

    @Test
    @DisplayName("32 线程并发共享同一个生成器实例，答案不能互相污染")
    void sharedGeneratorIsThreadSafe() throws Exception {
        CaptchaGenerator generator = new PngCaptchaGenerator();
        Set<String> codes = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = new ArrayList<>();

        runConcurrently(() -> {
            for (int i = 0; i < PER_THREAD; i++) {
                CaptchaResult result = generator.generate(CONFIG);
                if (result.code() == null || result.code().length() != CONFIG.length()) {
                    throw new AssertionError("并发下拿到非法答案：" + result.code());
                }
                codes.add(result.code());
            }
        }, errors);

        assertEquals(List.of(), errors, () -> "并发生成抛异常：" + errors);
        int total = THREADS * PER_THREAD;
        // 4 位、54 字符集，1600 次采样理论唯一值接近 1600；这里只要求显著高于单线程单值
        assertTrue(codes.size() > total * 0.9,
                () -> "并发下只产出 " + codes.size() + "/" + total + " 种答案，存在答案串号");
    }

    @Test
    @DisplayName("GIF 生成器并发也安全")
    void gifGeneratorIsThreadSafe() throws Exception {
        CaptchaGenerator generator = new GifCaptchaGenerator();
        Set<String> codes = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = new ArrayList<>();

        runConcurrently(() -> {
            for (int i = 0; i < 10; i++) {
                codes.add(generator.generate(CONFIG).code());
            }
        }, errors);

        assertEquals(List.of(), errors);
        assertTrue(codes.size() > 100, () -> "GIF 并发只产出 " + codes.size() + " 种答案");
    }

    @Test
    @DisplayName("算术生成器并发答案全部合法")
    void arithmeticGeneratorIsThreadSafe() throws Exception {
        CaptchaGenerator generator = new ArithmeticCaptchaGenerator();
        List<Throwable> errors = new ArrayList<>();

        runConcurrently(() -> {
            for (int i = 0; i < PER_THREAD; i++) {
                int answer = Integer.parseInt(generator.generate(CONFIG).code());
                if (answer < 0 || answer > 100) {
                    throw new AssertionError("算数答案越界：" + answer);
                }
            }
        }, errors);

        assertEquals(List.of(), errors);
    }

    private static void runConcurrently(Runnable task, List<Throwable> errors) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        try {
            for (int i = 0; i < THREADS; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (Throwable t) {
                        synchronizationLock(errors).add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(120, TimeUnit.SECONDS), "并发任务超时未完成");
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<Throwable> synchronizationLock(List<Throwable> errors) {
        synchronized (errors) {
            return errors;
        }
    }
}
