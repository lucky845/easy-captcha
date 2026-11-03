package io.github.lucky845.basic.core.behavior;

/**
 * 行为校验的阈值配置。
 *
 * <h3>设计原则：宁可漏放，不可误杀</h3>
 * <p>验证码误杀真人的代价远大于偶尔放进一个脚本：
 * 被挡住的用户会直接流失，而漏放的脚本还有限流、一次性校验、二次票据三层兜底。
 * 因此默认阈值刻意设得很宽松。
 *
 * <h3>为什么拖动和点击要分开</h3>
 * <p>这两类交互产生的数据形状完全不同，用同一套规则必然误杀：
 * <ul>
 *     <li><b>拖动</b>（滑块）：连续的 pointermove 轨迹，有位移和耗时，
 *         可以看"点够不够多""有没有来回横跳"；但<b>不能看弯曲度</b>——
 *         很多人就是笔直拖过去的，低采样率设备也只有几个点。</li>
 *     <li><b>点击</b>（点选）：每次点击只有一个点，本质上是一串离散坐标，
 *         点与点之间的"路径"根本不存在，所以弯曲度、方向单调性都<b>无意义</b>。</li>
 * </ul>
 *
 * @param minDurationMs      最短耗时（毫秒），低于此值判定为机器瞬移
 * @param maxDurationMs      最长耗时（毫秒），超过判定为超时
 * @param minPoints          最少轨迹点数（仅拖动模式有意义）
 * @param monotonicTolerance 允许的 X 方向回退像素（仅拖动模式有意义）
 * @param checkCurvature     是否启用弯曲度检查（默认关闭）
 * @param minCurvature       弯曲度下限，仅在启用检查时生效
 * @author created by lucky845
 */
public record BehaviorTuning(
        long minDurationMs,
        long maxDurationMs,
        int minPoints,
        int monotonicTolerance,
        boolean checkCurvature,
        double minCurvature
) {

    /**
     * 拖动型（滑块）默认阈值。
     *
     * <h4>为什么不设耗时下限</h4>
     * <p>耗时下限曾经设过 500ms、150ms、100ms，每次都在真实场景里误杀用户：
     * 快手用户、内网部署、CDN 回源都能在几十毫秒内完成提交。
     * 而"从签发到提交"的时间受网络影响，<b>与用户是不是机器人无关</b>，
     * 拿它做下限本质上是在猜网络速度。
     *
     * <p>真正可靠的机器特征只有两类：<b>答案错误</b>与<b>轨迹异常</b>，
     * 这两条已经覆盖了暴力破解和脚本拖拽。耗时只保留<b>上限</b>，用于拦截
     * "拿到验证码后挂机很久再提交"的重放场景。
     *
     * <p>因此 minDurationMs 默认 0 表示不设下限。
     */
    public static BehaviorTuning dragDefaults() {
        return new BehaviorTuning(0L, 300_000L, 1, 60, false, 1.0);
    }

    /**
     * 点击型（点选）默认阈值。
     *
     * <p>同 {@link #dragDefaults()}：不设耗时下限，只保留上限与坐标校验。
     */
    public static BehaviorTuning clickDefaults() {
        return new BehaviorTuning(0L, 300_000L, 1, Integer.MAX_VALUE, false, 1.0);
    }

    /**
     * 需要更严格时使用：对耗时下限有明确要求的场景。
     *
     * <p>仅在确认自己的用户网络延迟稳定、且风险足够高时才启用。
     *
     * @param minDurationMs 最短耗时
     */
    public static BehaviorTuning withMinDuration(long minDurationMs) {
        return new BehaviorTuning(minDurationMs, 300_000L, 1, 60, false, 1.0);
    }

    /**
     * 宽容模式（<b>默认</b>）：几乎只拦"答案错"和"轨迹大幅横跳"。
     *
     * <p>等价于 {@link #dragDefaults()}。适合绝大多数业务场景。
     */
    public static BehaviorTuning lenient() {
        return dragDefaults();
    }

    /**
     * 严格模式，适合高风险场景（如提现、改密码、批量注册）。
     *
     * <p>相对宽容模式的变化：
     * <ul>
     *     <li>启用弯曲度检查，要求轨迹不能是完美直线；</li>
     *     <li>要求至少 4 个轨迹点；</li>
     *     <li>回退容差从 60px 收紧到 20px；</li>
     *     <li>超时从 5 分钟收紧到 3 分钟。</li>
     * </ul>
     *
     * <p><b>仍然不设耗时下限</b>——那是误杀的主要来源。
     * 想更严请优先调低限流配额，而不是收紧轨迹。
     */
    public static BehaviorTuning strict() {
        return new BehaviorTuning(0L, 180_000L, 4, 20, true, 1.005);
    }

    /**
     * 兼容旧调用，等价于 {@link #dragDefaults()}。
     *
     * @deprecated 语义不明确，请改用 {@link #dragDefaults()} 或 {@link #clickDefaults()}
     */
    @Deprecated(since = "1.0.3")
    public static BehaviorTuning defaults() {
        return dragDefaults();
    }

    public BehaviorTuning {
        if (minDurationMs < 0) {
            throw new IllegalArgumentException("minDurationMs 不能为负");
        }
        if (maxDurationMs <= minDurationMs) {
            throw new IllegalArgumentException("maxDurationMs 必须大于 minDurationMs");
        }
        if (minPoints < 0) {
            throw new IllegalArgumentException("minPoints 不能为负");
        }
        if (monotonicTolerance < 0) {
            throw new IllegalArgumentException("monotonicTolerance 不能为负");
        }
        if (minCurvature < 1.0) {
            throw new IllegalArgumentException("minCurvature 不能小于 1.0");
        }
    }
}
