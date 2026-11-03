package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.behavior.BehaviorAnswer;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.InMemoryCaptchaStore;
import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import io.github.lucky845.basic.core.behavior.BehaviorVerdict;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为验证码、限流、二次校验的端到端测试。
 *
 * @author created by lucky845
 */
@DisplayName("行为验证码与二次校验")
class BehaviorCaptchaTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CaptchaAutoconfiguration.class));

    /**
     * 可手动推进的时钟，避免测试靠 sleep 等待真实时间。
     */
    private static final class MutableClock extends Clock {
        private final AtomicLong millis = new AtomicLong(Instant.now().toEpochMilli());

        void advance(long deltaMs) {
            millis.addAndGet(deltaMs);
        }

        @Override
        public long millis() {
            return millis.get();
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    /**
     * 构造一个使用可控时钟的服务，用于精确验证耗时判定。
     */
    private static CaptchaService serviceWithClock(MutableClock clock) {
        CaptchaProperties props = new CaptchaProperties();
        return new CaptchaService(
                new io.github.lucky845.basic.captcha.generator.CaptchaGeneratorRegistry(List.of()),
                props,
                new InMemoryCaptchaStore(props.getTtl(), props.getMaxEntries(), clock),
                BehaviorTuning.dragDefaults(),
                BehaviorTuning.clickDefaults(),
                new io.github.lucky845.basic.captcha.behavior.CaptchaRateLimiter(Integer.MAX_VALUE, 60_000L),
                clock);
    }

    /**
     * 构造一条像人手的轨迹。
     */
    private static List<BehaviorAnswer.Point> humanPoints(int from, int to) {
        List<BehaviorAnswer.Point> points = new ArrayList<>();
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            int x = (int) Math.round(from + (to - from) * ratio);
            int y = (int) Math.round(Math.sin(i * 1.7) * 3);
            points.add(new BehaviorAnswer.Point(x, y, i * 60L));
        }
        return points;
    }

    @Test
    @DisplayName("滑块：拖动到正确位置且轨迹像人 → 通过")
    void sliderCorrectAndHumanPasses() {
        MutableClock clock = new MutableClock();
        CaptchaService service = serviceWithClock(clock);
        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int target = Integer.parseInt(issue.result().code());

        // 模拟真人在签发后 1.2 秒才提交
        clock.advance(1200);

        BehaviorAnswer answer = new BehaviorAnswer(
                String.valueOf(target), humanPoints(0, target), 16 * 60L);

        BehaviorVerdict verdict = service.validateBehavior(issue.id(), answer);
        assertTrue(verdict.passed(), () -> "正确的滑块被判错：" + verdict.detail());
    }

    @Test
    @DisplayName("滑块：偏移量偏差过大 → 拒绝")
    void sliderWrongOffsetRejected() {
        MutableClock clock = new MutableClock();
        CaptchaService service = serviceWithClock(clock);
        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int target = Integer.parseInt(issue.result().code());
        clock.advance(1000);

        BehaviorAnswer answer = new BehaviorAnswer(
                String.valueOf(target + 40), humanPoints(0, target + 40), 1000);

        BehaviorVerdict verdict = service.validateBehavior(issue.id(), answer);
        assertFalse(verdict.passed());
        assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, verdict.reason());
    }

    @Test
    @DisplayName("滑块：伪造客户端时间也骗不过服务端时钟（回归：客户端时间可伪造）")
    void forgedClientTimingDoesNotBypass() {
        runner.withPropertyValues("easy-captcha.behavior.drag.min-duration-ms=5000")
                .run(context -> {
                    CaptchaService service = context.getBean(CaptchaService.class);
                    CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                    int target = Integer.parseInt(issue.result().code());

                    // 脚本把 duration 和每个点的 t 都伪造成 10 分钟，
                    // 但服务端从签发到此刻只过了几十毫秒 —— 必须按服务端时间拒绝
                    List<BehaviorAnswer.Point> forged = new ArrayList<>();
                    for (int i = 0; i <= 10; i++) {
                        forged.add(new BehaviorAnswer.Point(target * i / 10, 0, 600_000L));
                    }
                    BehaviorAnswer answer = new BehaviorAnswer(String.valueOf(target), forged, 600_000);

                    BehaviorVerdict verdict = service.validateBehavior(issue.id(), answer);
                    assertFalse(verdict.passed(), "客户端伪造的时间不应骗过服务端时钟");
                    assertEquals(BehaviorVerdict.Reason.TOO_FAST, verdict.reason(),
                            "应按服务端真实经过时间判定为过快");
                });
    }

    @Test
    @DisplayName("滑块：真人正常操作必然通过（回归：误杀真人）")
    void genuineUserAlwaysPasses() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);

            // 覆盖从手快（几乎立即提交）到慢慢拖的整个区间
            for (long thinkMs : new long[]{50, 150, 300, 600, 1200}) {
                CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                int target = Integer.parseInt(issue.result().code());

                if (thinkMs > 0) {
                    try {
                        Thread.sleep(thinkMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // 直线拖动、只上报少量点 —— 这些都是真人的常见形态
                List<BehaviorAnswer.Point> points = List.of(
                        new BehaviorAnswer.Point(0, 0, 0),
                        new BehaviorAnswer.Point(target / 2, 2, 200),
                        new BehaviorAnswer.Point(target, 1, 400));

                BehaviorVerdict verdict = service.validateBehavior(
                        issue.id(), new BehaviorAnswer(String.valueOf(target), points, 400));
                assertTrue(verdict.passed(),
                        () -> "思考 " + thinkMs + "ms 的真人被误杀：" + verdict.reason() + " / " + verdict.detail());
            }
        });
    }

    @Test
    @DisplayName("点选：真人连续快速点击必须通过（回归：误杀真人）")
    void genuineClickUserPasses() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            for (long thinkMs : new long[]{50, 120, 300}) {
                CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);
                if (thinkMs > 0) {
                    try {
                        Thread.sleep(thinkMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                // 三次点击间隔很短，总共 240ms —— 手快的真人就是这个速度
                List<BehaviorAnswer.Point> points = List.of(
                        new BehaviorAnswer.Point(40, 25, 0),
                        new BehaviorAnswer.Point(110, 30, 120),
                        new BehaviorAnswer.Point(160, 22, 240));

                BehaviorVerdict verdict = service.validateBehavior(issue.id(),
                        new BehaviorAnswer(issue.result().code(), points, 240));
                assertTrue(verdict.passed(),
                        () -> "思考 " + thinkMs + "ms 的点选用户被误杀：" + verdict.reason() + " / " + verdict.detail());
            }
        });
    }

    @Test
    @DisplayName("滑块：验证码被重放 → 第二次拒绝")
    void sliderReplayRejected() {
        MutableClock clock = new MutableClock();
        CaptchaService service = serviceWithClock(clock);
        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
        int target = Integer.parseInt(issue.result().code());
        clock.advance(1000);

        BehaviorAnswer answer = new BehaviorAnswer(
                String.valueOf(target), humanPoints(0, target), 1000);

        assertTrue(service.validateBehavior(issue.id(), answer).passed());
        BehaviorVerdict second = service.validateBehavior(issue.id(), answer);
        assertFalse(second.passed(), "同一个验证码不应能被二次使用");
        assertEquals(BehaviorVerdict.Reason.INVALID_INPUT, second.reason());
    }

    @Test
    @DisplayName("点选：按正确顺序点击 → 通过")
    void clickCorrectOrderPasses() {
        MutableClock clock = new MutableClock();
        CaptchaService service = serviceWithClock(clock);
        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);
        clock.advance(600);

        String answer = issue.result().code();
        List<BehaviorAnswer.Point> points = humanPoints(0, 120);

        BehaviorVerdict verdict = service.validateBehavior(
                issue.id(), new BehaviorAnswer(answer, points, 1200));
        assertTrue(verdict.passed(), () -> "正确点选被拒：" + verdict.detail());
    }

    @Test
    @DisplayName("点选：坐标顺序颠倒 → 拒绝")
    void clickWrongOrderRejected() {
        MutableClock clock = new MutableClock();
        CaptchaService service = serviceWithClock(clock);
        {
            CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);
            clock.advance(600);

            String[] pairs = issue.result().code().split(";");
            // 反转顺序
            StringBuilder reversed = new StringBuilder();
            for (int i = pairs.length - 1; i >= 0; i--) {
                if (reversed.length() > 0) {
                    reversed.append(';');
                }
                reversed.append(pairs[i]);
            }

            BehaviorVerdict verdict = service.validateBehavior(
                    issue.id(), new BehaviorAnswer(reversed.toString(), humanPoints(0, 120), 1200));
            assertFalse(verdict.passed(), "顺序颠倒不应通过");
            assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, verdict.reason());
        }
    }

    @Test
    @DisplayName("滑块与点选的渲染数据可下发，且不含答案")
    void challengeDataIsPublicSafe() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);

            CaptchaResult slider = service.generate(CaptchaTypeEnum.SLIDER_CAPTCHA);
            assertTrue(slider.extra().containsKey("slider"), "滑块缺少渲染数据");
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> sliderData = (java.util.Map<String, Object>) slider.extra().get("slider");
            assertTrue(sliderData.containsKey("background"));
            assertTrue(sliderData.containsKey("block"));
            // 关键安全断言：答案不能出现在下发给前端的数据里
            String answer = slider.code();
            assertFalse(sliderData.toString().contains("\"" + answer + "\""),
                    "答案泄露进了前端渲染数据");

            CaptchaResult click = service.generate(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);
            assertTrue(click.extra().containsKey("prompt"), "点选缺少提示文字");
            assertFalse(click.extra().containsKey("points"), "坐标答案不应下发");
        });
    }

    @Test
    @DisplayName("二次校验：校验通过换令牌，令牌可验一次，业务接口才放行")
    void ticketFlow() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.PNG_CAPTCHA);

            // 答案错 → 拿不到令牌
            assertTrue(service.validateAndIssueToken(issue.id(), "__wrong__").isEmpty());

            // 换一张，用正确答案换令牌
            CaptchaService.CaptchaIssue second = service.issue(CaptchaTypeEnum.PNG_CAPTCHA);
            String token = service.validateAndIssueToken(second.id(), second.result().code())
                    .orElseThrow(() -> new AssertionError("正确答案未能换到令牌"));

            assertTrue(service.verifyToken(token), "首次验票应当通过");
            assertFalse(service.verifyToken(token), "令牌不应能重复使用");
        });
    }

    @Test
    @DisplayName("二次校验：空令牌无效")
    void emptyTicketInvalid() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            assertFalse(service.verifyToken(null));
            assertFalse(service.verifyToken(""));
            assertFalse(service.verifyToken("   "));
            assertFalse(service.verifyToken("never-issued"));
        });
    }

    @Test
    @DisplayName("限流：超过配额后拒绝签发并给出异常的剩余次数")
    void rateLimitKicksIn() {
        runner.withPropertyValues(
                        "easy-captcha.rate-limit-permits=3",
                        "easy-captcha.rate-limit-window=1m")
                .run(context -> {
                    CaptchaService service = context.getBean(CaptchaService.class);
                    String key = "192.0.2.1";

                    for (int i = 0; i < 3; i++) {
                        service.issue(CaptchaTypeEnum.PNG_CAPTCHA, key);
                    }
                    assertEquals(0, service.remainingQuota(key));

                    assertTrue(
                            org.junit.jupiter.api.Assertions.assertThrows(
                                            io.github.lucky845.basic.core.exception.RateLimitExceededException.class,
                                            () -> service.issue(CaptchaTypeEnum.PNG_CAPTCHA, key))
                                    .getMessage().contains("频繁"),
                            "超限应抛限流异常");
                });
    }

    @Test
    @DisplayName("限流：不同 key 互不影响")
    void rateLimitIsPerKey() {
        runner.withPropertyValues("easy-captcha.rate-limit-permits=2")
                .run(context -> {
                    CaptchaService service = context.getBean(CaptchaService.class);
                    service.issue(CaptchaTypeEnum.PNG_CAPTCHA, "a");
                    service.issue(CaptchaTypeEnum.PNG_CAPTCHA, "a");
                    // a 已用完，b 仍可签发
                    service.issue(CaptchaTypeEnum.PNG_CAPTCHA, "b");
                    assertEquals(1, service.remainingQuota("b"));
                });
    }

    @Test
    @DisplayName("对静态验证码调用行为校验会被明确拒绝而不是误判通过")
    void behaviorOnStaticTypeRejected() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.PNG_CAPTCHA);

            BehaviorVerdict verdict = service.validateBehavior(
                    issue.id(), new BehaviorAnswer(issue.result().code(), humanPoints(0, 50), 1000));
            assertFalse(verdict.passed());
            assertEquals(BehaviorVerdict.Reason.INVALID_INPUT, verdict.reason());
        });
    }

    @Test
    @DisplayName("行为验证码也能通过 /captcha/image 接口直接出图")
    void behaviorTypesProduceImage() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            for (CaptchaTypeEnum type : new CaptchaTypeEnum[]{
                    CaptchaTypeEnum.SLIDER_CAPTCHA, CaptchaTypeEnum.CLICK_WORD_CAPTCHA}) {
                CaptchaResult result = service.generate(type);
                assertTrue(result.size() > 1000, () -> type + " 图片过小: " + result.size());
            }
        });
    }
}
