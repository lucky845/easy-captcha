package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.behavior.BehaviorAnswer;
import io.github.lucky845.basic.captcha.behavior.CaptchaRateLimiter;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorRegistry;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.CaptchaStore;
import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import io.github.lucky845.basic.core.behavior.BehaviorVerdict;
import io.github.lucky845.basic.core.behavior.Trajectory;
import io.github.lucky845.basic.core.behavior.TrajectoryValidator;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;

import java.util.ArrayList;
import java.util.List;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 验证码服务：生成入口 + 一次性校验 + 行为校验 + 二次校验令牌。
 *
 * <p>本类**不持有任何请求级状态**。每次调用都直接返回新生成的 {@link CaptchaResult}，
 * 需要"生成 → 校验"配对时使用 {@link #issue()}，由 {@link CaptchaStore} 以 id 保存答案。
 * 因此可以安全地被任意数量的并发请求共享。
 *
 * <h3>三种校验姿势</h3>
 * <ol>
 *     <li>{@link #validate}：一次性校验，校验后即失效。适合"看图输入"型验证码。</li>
 *     <li>{@link #validateBehavior}：带轨迹的行为校验。适合滑块、点选。</li>
 *     <li>{@link #verifyToken}：二次校验（AJ-Captcha 模式）。第一次校验通过后换发一个
 *         短命的 token，业务接口（登录/注册/发短信）拿着 token 再验一次，确保
 *         "验证码过了"这件事不能被跳过、也不能被重放。</li>
 * </ol>
 *
 * @author created by lucky845 on 2025-01-16
 */
public class CaptchaService {

    private static final Logger LOG = Logger.getLogger(CaptchaService.class.getName());

    /**
     * 二次校验令牌的前缀，用于和普通验证码 id 区分
     */
    private static final String TOKEN_PREFIX = "ticket:";

    private final CaptchaGeneratorRegistry registry;

    private final CaptchaProperties properties;

    private final CaptchaStore store;

    private final BehaviorTuning dragTuning;

    private final BehaviorTuning clickTuning;

    private final CaptchaRateLimiter rateLimiter;

    /**
     * 时钟。生产使用系统时钟；测试可注入可控时钟来模拟时间流逝，
     * 从而在不 sleep 的前提下验证耗时相关的判定。
     */
    private final Clock clock;

    public CaptchaService(CaptchaGeneratorRegistry registry,
                          CaptchaProperties properties,
                          CaptchaStore store,
                          BehaviorTuning dragTuning,
                          BehaviorTuning clickTuning,
                          CaptchaRateLimiter rateLimiter) {
        this(registry, properties, store, dragTuning, clickTuning, rateLimiter, Clock.systemUTC());
    }

    public CaptchaService(CaptchaGeneratorRegistry registry,
                          CaptchaProperties properties,
                          CaptchaStore store,
                          BehaviorTuning dragTuning,
                          BehaviorTuning clickTuning,
                          CaptchaRateLimiter rateLimiter,
                          Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.store = store;
        this.dragTuning = dragTuning;
        this.clickTuning = clickTuning;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /**
     * 兼容旧构造：使用默认阈值，不限流。
     */
    public CaptchaService(CaptchaGeneratorRegistry registry, CaptchaProperties properties, CaptchaStore store) {
        this(registry, properties, store,
                BehaviorTuning.dragDefaults(), BehaviorTuning.clickDefaults(),
                new CaptchaRateLimiter(Integer.MAX_VALUE, 60_000L));
    }

    /**
     * 按配置的默认类型生成验证码，不落库。
     */
    public CaptchaResult generate() {
        return generate(properties.getCaptchaType());
    }

    /**
     * 按指定类型生成验证码，不落库。
     *
     * @param type 验证码类型
     */
    public CaptchaResult generate(CaptchaTypeEnum type) {
        // 滑块拼图对尺寸敏感：块太小会让凸舌看不清，用户无法判断有没有对准，
        // 所以它单独用 slider-width / slider-height，而不是全局宽高。
        boolean forSlider = type == CaptchaTypeEnum.SLIDER_CAPTCHA;
        return registry.get(type).generate(properties.toCaptchaConfig(forSlider));
    }

    /**
     * 按配置的默认类型生成验证码并存入 {@link CaptchaStore}。
     *
     * @return 验证码 id 与结果
     */
    public CaptchaIssue issue() {
        return issue(properties.getCaptchaType());
    }

    /**
     * 按指定类型生成验证码并存入 {@link CaptchaStore}。
     *
     * @param type 验证码类型
     * @return 验证码 id 与结果
     */
    public CaptchaIssue issue(CaptchaTypeEnum type) {
        CaptchaResult result = generate(type);
        String id = newId("");
        store.save(id, result);
        return new CaptchaIssue(id, result);
    }

    /**
     * 带限流的签发。超限时抛出 {@link io.github.lucky845.basic.core.exception.RateLimitExceededException}。
     *
     * @param type 类型
     * @param key  限流维度（通常是客户端 IP）
     */
    public CaptchaIssue issue(CaptchaTypeEnum type, String key) {
        if (!rateLimiter.tryAcquire(key)) {
            throw new io.github.lucky845.basic.core.exception.RateLimitExceededException(
                    "请求过于频繁，请稍后再试");
        }
        return issue(type);
    }

    /**
     * 校验用户输入。校验成功后该验证码立即失效（一次性）。
     *
     * @param id     验证码 id
     * @param answer 用户提交的答案
     * @return 是否通过
     */
    public boolean validate(String id, String answer) {
        return checkText(id, answer).passed();
    }

    /**
     * 校验并返回详细结果，便于区分"答案错"和"验证码已过期"。
     *
     * @param id     验证码 id
     * @param answer 用户提交的答案
     */
    public BehaviorVerdict checkText(String id, String answer) {
        if (id == null || id.isBlank() || answer == null) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "id 或答案为空");
        }
        Optional<CaptchaResult> stored = store.remove(id);
        if (stored.isEmpty()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "验证码不存在或已过期");
        }
        String expected = stored.get().code();
        if (!expected.equalsIgnoreCase(answer.trim())) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.ANSWER_MISMATCH, "答案不匹配");
        }
        return BehaviorVerdict.pass();
    }

    /**
     * 行为验证码校验：答案 + 轨迹一起判定。
     *
     * @param id     验证码 id
     * @param answer 前端提交的答案与轨迹
     * @return 裁决
     */
    public BehaviorVerdict validateBehavior(String id, BehaviorAnswer answer) {
        if (id == null || id.isBlank() || answer == null) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "id 或提交内容为空");
        }
        Optional<CaptchaStore.Issued> taken = store.take(id);
        if (taken.isEmpty()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "验证码不存在或已过期");
        }
        CaptchaResult expected = taken.get().result();
        // 耗时口径：只用服务端时钟。
        //
        // 客户端上报的 duration 和轨迹点时间戳都可被脚本任意伪造——曾经用它们做下限，
        // 结果脚本填一个假的 t=5000 就能绕过。唯一不可伪造的是"签发到提交之间
        // 服务端真实经过了多久"。
        //
        // 用服务端时钟做下限为什么安全：网络延迟只会让它<b>变大</b>，
        // 而变大是宽松方向——只会少拦，绝不会误杀真人。
        long serverElapsed = Math.max(0, clock.millis() - taken.get().issuedAt());
        Trajectory trajectory = new Trajectory(toCorePoints(answer.points()), serverElapsed);

        return switch (expectedTypeOf(expected)) {
            case SLIDER_CAPTCHA -> validateSlider(expected, answer, trajectory);
            case CLICK_WORD_CAPTCHA -> validateClick(expected, answer, trajectory);
            default -> BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT,
                    "该验证码类型不是行为验证码，请使用 validate()");
        };
    }

    /**
     * 二次校验：用第一次校验换来的 token 再验一次。
     *
     * @param token 令牌
     * @return 是否有效（有效即消费掉，不能重复使用）
     */
    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return store.remove(TOKEN_PREFIX + token).isPresent();
    }

    /**
     * 校验通过后签发二次校验令牌。
     *
     * @param id     验证码 id
     * @param answer 用户答案
     * @return 令牌，校验失败时为 {@link Optional#empty()}
     */
    public Optional<String> validateAndIssueToken(String id, String answer) {
        if (!validate(id, answer)) {
            return Optional.empty();
        }
        String token = newId("");
        store.save(TOKEN_PREFIX + token, new CaptchaResult("ticket", new byte[]{1},
                io.github.lucky845.basic.core.enums.ContentTypeEnum.PNG));
        return Optional.of(token);
    }

    /**
     * 当前未被消费的验证码数量。
     */
    public int pendingCount() {
        return store.size();
    }

    /**
     * 剩余可用配额。
     *
     * @param key 限流维度
     */
    public int remainingQuota(String key) {
        return rateLimiter.remaining(key);
    }

    private BehaviorVerdict validateSlider(CaptchaResult expected, BehaviorAnswer answer, Trajectory trajectory) {
        int expectedOffset = parseIntSafe(expected.code());
        if (expectedOffset < 0) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "服务端答案损坏");
        }
        int submitted = parseIntSafe(answer.answer());
        if (submitted < 0) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "偏移量格式非法：" + answer.answer());
        }
        return TrajectoryValidator.validateSlider(
                expectedOffset, submitted, properties.getSliderTolerance(), trajectory, dragTuning);
    }

    private BehaviorVerdict validateClick(CaptchaResult expected, BehaviorAnswer answer, Trajectory trajectory) {
        List<Trajectory.Point> want = parsePoints(expected.code());
        List<Trajectory.Point> got = parsePoints(answer.answer());
        if (want.isEmpty() || got.isEmpty()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "点选坐标格式非法");
        }
        return TrajectoryValidator.validateClick(
                want, got, properties.getClickTolerance(), trajectory, clickTuning);
    }

    /**
     * 从结果里推断类型：行为验证码的 extra 中带有类型标记。
     */
    private CaptchaTypeEnum expectedTypeOf(CaptchaResult result) {
        Object type = result.extra().get("captchaType");
        if (type instanceof CaptchaTypeEnum e) {
            return e;
        }
        if (result.extra().containsKey("slider")) {
            return CaptchaTypeEnum.SLIDER_CAPTCHA;
        }
        if (result.extra().containsKey("prompt")) {
            return CaptchaTypeEnum.CLICK_WORD_CAPTCHA;
        }
        return CaptchaTypeEnum.PNG_CAPTCHA;
    }

    private static List<Trajectory.Point> toCorePoints(List<BehaviorAnswer.Point> points) {
        List<Trajectory.Point> list = new ArrayList<>(points.size());
        for (BehaviorAnswer.Point p : points) {
            list.add(new Trajectory.Point(p.x(), p.y(), p.t()));
        }
        return list;
    }

    /**
     * 解析 "x,y;x,y" 形式的坐标串。
     */
    private static List<Trajectory.Point> parsePoints(String raw) {
        List<Trajectory.Point> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return list;
        }
        for (String pair : raw.split(";")) {
            String[] xy = pair.trim().split(",");
            if (xy.length != 2) {
                return List.of();
            }
            int x = parseIntSafe(xy[0].trim());
            int y = parseIntSafe(xy[1].trim());
            if (x < 0 || y < 0) {
                return List.of();
            }
            list.add(new Trajectory.Point(x, y, 0));
        }
        return list;
    }

    private static int parseIntSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成结果的载体：id 用来回传校验，result 用来返回图片与答案。
     *
     * @param id     验证码 id
     * @param result 验证码结果
     */
    public record CaptchaIssue(String id, CaptchaResult result) {
    }
}
