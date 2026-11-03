package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.behavior.BehaviorAnswer;
import io.github.lucky845.basic.captcha.behavior.CaptchaRateLimiter;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorRegistry;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.CaptchaStore;
import io.github.lucky845.basic.captcha.store.RedisCaptchaStore;
import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import io.github.lucky845.basic.core.behavior.BehaviorVerdict;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 通过 Redis 存储走完整链路。
 *
 * <p>这里用一个内存字典冒充 Redis 的 {@code SET}/{@code GETDEL}，重点不是测 Redis 本身，
 * 而是验证**序列化之后校验所需的信息一个都没丢**：答案、图片、签发时刻、行为验证码的
 * 附加数据。这些信息只要丢一个，多副本部署下校验就会失败。
 *
 * <p>最有价值的是「两个实例共享同一个 Redis」那条：这正是内存存储撑不住的场景。
 *
 * @author created by lucky845
 */
@DisplayName("Redis 存储端到端")
class RedisCaptchaPipelineTest {

    /**
     * 冒充 Redis：key → value，SET 时记下 TTL。
     */
    private static final class FakeRedis {

        private final Map<String, String> data = new ConcurrentHashMap<>();

        private final Map<String, Duration> ttls = new ConcurrentHashMap<>();

        StringRedisTemplate template() {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            @SuppressWarnings("unchecked")
            ValueOperations<String, String> ops = mock(ValueOperations.class);

            when(redis.opsForValue()).thenReturn(ops);
            // set 是 void 方法，只能用 doAnswer 打桩
            doAnswer(inv -> {
                data.put(inv.getArgument(0), inv.getArgument(1));
                ttls.put(inv.getArgument(0), inv.getArgument(2));
                return null;
            }).when(ops).set(anyString(), anyString(), any(Duration.class));
            when(ops.getAndDelete(anyString()))
                    .thenAnswer(inv -> data.remove((String) inv.getArgument(0)));
            when(redis.scan(any())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                Cursor<String> cursor = mock(Cursor.class);
                when(cursor.hasNext()).thenReturn(false);
                return cursor;
            });
            return redis;
        }
    }

    private static CaptchaService service(CaptchaStore store) {
        CaptchaProperties props = new CaptchaProperties();
        return new CaptchaService(
                new CaptchaGeneratorRegistry(List.of()),
                props,
                store,
                BehaviorTuning.dragDefaults(),
                BehaviorTuning.clickDefaults(),
                new CaptchaRateLimiter(Integer.MAX_VALUE, 60_000L),
                Clock.systemUTC());
    }

    private static List<BehaviorAnswer.Point> humanPoints(int to) {
        List<BehaviorAnswer.Point> points = new ArrayList<>();
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            points.add(new BehaviorAnswer.Point(
                    (int) Math.round(to * ratio),
                    (int) Math.round(Math.sin(i * 1.7) * 3),
                    i * 60L));
        }
        return points;
    }

    @Test
    @DisplayName("滑块：签发 → 经 Redis 序列化 → 正确偏移 + 真人轨迹 → 通过")
    void sliderPassesThroughRedis() {
        FakeRedis fake = new FakeRedis();
        CaptchaService service = service(new RedisCaptchaStore(fake.template(), Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int answer = Integer.parseInt(issued.result().code());

        BehaviorVerdict verdict = service.validateBehavior(issued.id(),
                new BehaviorAnswer(String.valueOf(answer), humanPoints(answer), 960));

        assertTrue(verdict.passed(), "经 Redis 存取后仍应通过，实际：" + verdict.reason() + " " + verdict.detail());
    }

    @Test
    @DisplayName("两个实例共享同一个 Redis：A 签发，B 校验成功")
    void crossInstanceValidation() {
        FakeRedis fake = new FakeRedis();
        StringRedisTemplate redis = fake.template();

        CaptchaService instanceA = service(new RedisCaptchaStore(redis, Duration.ofMinutes(2)));
        CaptchaService instanceB = service(new RedisCaptchaStore(redis, Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = instanceA.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int answer = Integer.parseInt(issued.result().code());

        // 用户在 A 拿到验证码，提交时被负载均衡打到了 B
        BehaviorVerdict verdict = instanceB.validateBehavior(issued.id(),
                new BehaviorAnswer(String.valueOf(answer), humanPoints(answer), 960));

        assertTrue(verdict.passed(), "多副本部署下跨实例校验必须成功，实际：" + verdict.reason());
    }

    @Test
    @DisplayName("跨实例也保持一次性：A 签发、B 校验通过后，A 再校验失败")
    void oneShotAcrossInstances() {
        FakeRedis fake = new FakeRedis();
        StringRedisTemplate redis = fake.template();

        CaptchaService instanceA = service(new RedisCaptchaStore(redis, Duration.ofMinutes(2)));
        CaptchaService instanceB = service(new RedisCaptchaStore(redis, Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = instanceA.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int answer = Integer.parseInt(issued.result().code());
        BehaviorAnswer human = new BehaviorAnswer(String.valueOf(answer), humanPoints(answer), 960);

        assertTrue(instanceB.validateBehavior(issued.id(), human).passed());
        assertTrue(!instanceA.validateBehavior(issued.id(), human).passed(), "验证码只能通过一次");
    }

    @Test
    @DisplayName("文字点选：坐标串经 Redis 往返后仍能正确比对")
    void clickWordPassesThroughRedis() {
        FakeRedis fake = new FakeRedis();
        CaptchaService service = service(new RedisCaptchaStore(fake.template(), Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = service.issue(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);

        BehaviorVerdict verdict = service.validateBehavior(issued.id(),
                new BehaviorAnswer(issued.result().code(), humanPoints(120), 720));

        assertTrue(verdict.passed(), "点选应通过，实际：" + verdict.reason() + " " + verdict.detail());
    }

    @Test
    @DisplayName("答案错误仍然被拒，Redis 不会把判定变成放行")
    void wrongAnswerStillRejected() {
        FakeRedis fake = new FakeRedis();
        CaptchaService service = service(new RedisCaptchaStore(fake.template(), Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);

        BehaviorVerdict verdict = service.validateBehavior(issued.id(),
                new BehaviorAnswer(String.valueOf(Integer.parseInt(issued.result().code()) + 40),
                        humanPoints(40), 960));

        assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, verdict.reason());
    }

    @Test
    @DisplayName("并发抢同一张验证码：只有 1 个能通过")
    void concurrentTakesOnlyOneWins() throws Exception {
        FakeRedis fake = new FakeRedis();
        StringRedisTemplate redis = fake.template();
        // 用真正的原子性来源：ConcurrentHashMap.remove 本身就是原子的，
        // 与应用里 GETDEL 的语义一致。
        CaptchaService service = service(new RedisCaptchaStore(redis, Duration.ofMinutes(2)));

        CaptchaService.CaptchaIssue issued = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int answer = Integer.parseInt(issued.result().code());
        BehaviorAnswer human = new BehaviorAnswer(String.valueOf(answer), humanPoints(answer), 960);

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (service.validateBehavior(issued.id(), human).passed()) {
                        passed.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "并发校验超时");
        pool.shutdownNow();

        assertEquals(1, passed.get(), "一次性验证码在并发下只能通过一次，实际通过 " + passed.get() + " 次");
    }
}
