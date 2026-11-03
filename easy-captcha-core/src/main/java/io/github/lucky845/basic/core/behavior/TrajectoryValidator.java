package io.github.lucky845.basic.core.behavior;

import java.util.List;

/**
 * 行为轨迹校验器。
 *
 * <h3>核心原则：宁可漏放，不可误杀</h3>
 * <p>被误杀的真人会直接流失，而漏放的脚本还有限流、一次性销毁、二次票据三层兜底。
 * 所以这里只拦截<b>确定是机器</b>的特征，任何"有点像机器但也可能是人"的情况一律放行。
 *
 * <h3>拖动与点击使用不同规则</h3>
 * <p>两者的数据形状不同，混用一套规则是误杀的根源：
 * <ul>
 *     <li>拖动：有连续位移轨迹，可以看点数与方向；<b>不看弯曲度</b>（真人常拖直线）。</li>
 *     <li>点击：只有离散坐标，路径不存在，<b>只看总耗时</b>。</li>
 * </ul>
 *
 * @author created by lucky845
 */
public final class TrajectoryValidator {

    private TrajectoryValidator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 拖动型轨迹校验（滑块）。
     *
     * <p>只做三件事：耗时不能短到违反物理、点不能少到完全没采样、不能大幅来回横跳。
     *
     * @param trajectory 用户轨迹
     * @param tuning     阈值，通常来自 {@link BehaviorTuning#dragDefaults()}
     * @return 裁决
     */
    public static BehaviorVerdict validateDrag(Trajectory trajectory, BehaviorTuning tuning) {
        if (tuning == null) {
            tuning = BehaviorTuning.dragDefaults();
        }
        // 轨迹缺失时不做拦截：部分浏览器/隐私模式拿不到轨迹，
        // 直接拒绝会让这些用户永远无法通过。
        if (trajectory == null || trajectory.points().isEmpty()) {
            return BehaviorVerdict.pass();
        }
        return validateCommon(trajectory, tuning);
    }

    /**
     * 点击型轨迹校验（点选）。
     *
     * <p>点选只有离散坐标，点数、方向、弯曲度全部无意义，只看总耗时。
     *
     * @param trajectory 点击序列
     * @param tuning     阈值，通常来自 {@link BehaviorTuning#clickDefaults()}
     * @return 裁决
     */
    public static BehaviorVerdict validateClickBehavior(Trajectory trajectory, BehaviorTuning tuning) {
        if (tuning == null) {
            tuning = BehaviorTuning.clickDefaults();
        }
        if (trajectory == null || trajectory.points().isEmpty()) {
            return BehaviorVerdict.pass();
        }
        // 只信任显式传入的 duration。轨迹点里的 t 由客户端提供、可伪造，
        // 绝不能参与判定，否则脚本填一个假的 t 就能绕过或触发误判。
        long duration = trajectory.duration();
        // 同 validateCommon：0 表示同一毫秒内完成，是机器特征而非"无数据"
        if (duration < tuning.minDurationMs()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TOO_FAST,
                    "耗时 " + duration + "ms，低于下限 " + tuning.minDurationMs() + "ms");
        }
        if (duration > tuning.maxDurationMs()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TIMEOUT,
                    "耗时 " + duration + "ms，超过上限 " + tuning.maxDurationMs() + "ms");
        }
        return BehaviorVerdict.pass();
    }

    /**
     * 通用校验，供拖动模式使用。
     *
     * @param trajectory 轨迹
     * @param tuning     阈值
     * @return 裁决
     */
    public static BehaviorVerdict validate(Trajectory trajectory, BehaviorTuning tuning) {
        if (tuning == null) {
            tuning = BehaviorTuning.dragDefaults();
        }
        if (trajectory == null || trajectory.points().isEmpty()) {
            return BehaviorVerdict.pass();
        }
        return validateCommon(trajectory, tuning);
    }

    private static BehaviorVerdict validateCommon(Trajectory trajectory, BehaviorTuning tuning) {
        // 只信任显式传入的 duration。调用方（CaptchaService）传入的是服务端时钟
        // 计算出的真实经过时间；轨迹点里的 t 由客户端提供，可被伪造，绝不参与判定。
        //
        // 注意：不能写成 duration > 0 才判断下限。服务端时钟恒可用，
        // duration 为 0 意味着"签发与提交发生在同一毫秒内"，那本身正是机器特征。
        // 若这里放行 0，脚本只要跑得够快就能绕过全部耗时校验。
        long duration = trajectory.duration();

        // 1. 耗时下限
        if (duration < tuning.minDurationMs()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TOO_FAST,
                    "耗时 " + duration + "ms，低于下限 " + tuning.minDurationMs() + "ms");
        }
        // 2. 耗时上限
        if (duration > tuning.maxDurationMs()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TIMEOUT,
                    "耗时 " + duration + "ms，超过上限 " + tuning.maxDurationMs() + "ms");
        }
        // 3. 点数下限（仅在要求 > 1 时才检查，默认 1 表示不限制）
        if (tuning.minPoints() > 1 && !trajectory.hasEnoughPoints(tuning.minPoints())) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TOO_FEW_POINTS,
                    "轨迹点 " + trajectory.size() + " 个，少于下限 " + tuning.minPoints());
        }
        // 4. 方向单调性（容差很大，只拦大幅横跳）
        if (tuning.monotonicTolerance() < Integer.MAX_VALUE
                && !trajectory.xMonotonic(tuning.monotonicTolerance())) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.ABNORMAL_DIRECTION,
                    "轨迹 X 方向出现超过 " + tuning.monotonicTolerance() + "px 的回退");
        }
        // 5. 弯曲度：默认关闭。真人拖直线是常态，用弯曲度判断会大面积误杀。
        if (tuning.checkCurvature() && trajectory.size() >= 3
                && trajectory.curvature() < tuning.minCurvature()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.TOO_STRAIGHT,
                    String.format("轨迹弯曲度 %.4f，低于下限 %.4f",
                            trajectory.curvature(), tuning.minCurvature()));
        }
        return BehaviorVerdict.pass();
    }

    /**
     * 滑块完整校验：先验行为，再验偏移量。
     *
     * <p><b>顺序很关键。</b>如果先比对答案，攻击者就能通过"答案错"和"轨迹错"两种
     * 不同返回的差异，反复请求来探测真实偏移量。因此固定先做行为校验：
     * 轨迹不像人就直接拒绝，且不透露答案对不对。
     *
     * @param expectedOffset  服务端记录的正确偏移量
     * @param submittedOffset 前端提交的偏移量
     * @param tolerance       允许的像素误差
     * @param trajectory      用户轨迹
     * @param tuning          阈值
     * @return 裁决
     */
    public static BehaviorVerdict validateSlider(int expectedOffset,
                                                 int submittedOffset,
                                                 int tolerance,
                                                 Trajectory trajectory,
                                                 BehaviorTuning tuning) {
        if (tolerance < 0) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "容差不能为负");
        }
        BehaviorVerdict behavior = validateDrag(trajectory, tuning);
        if (!behavior.passed()) {
            return behavior;
        }
        int diff = Math.abs(expectedOffset - submittedOffset);
        if (diff > tolerance) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.ANSWER_MISMATCH,
                    "偏移量 " + submittedOffset + " 与预期相差 " + diff + "px，超过容差 " + tolerance + "px");
        }
        return BehaviorVerdict.pass();
    }

    /**
     * 点选完整校验：先验行为，再验坐标序列。
     *
     * @param expected   正确的点击坐标序列
     * @param submitted  前端提交的点击坐标序列
     * @param tolerance  每个点的允许误差（像素）
     * @param trajectory 用户轨迹
     * @param tuning     阈值
     * @return 裁决
     */
    public static BehaviorVerdict validateClick(List<Trajectory.Point> expected,
                                                List<Trajectory.Point> submitted,
                                                int tolerance,
                                                Trajectory trajectory,
                                                BehaviorTuning tuning) {
        if (expected == null || submitted == null) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.INVALID_INPUT, "点选坐标为空");
        }
        BehaviorVerdict behavior = validateClickBehavior(trajectory, tuning);
        if (!behavior.passed()) {
            return behavior;
        }
        if (expected.size() != submitted.size()) {
            return BehaviorVerdict.reject(BehaviorVerdict.Reason.ANSWER_MISMATCH,
                    "点击次数 " + submitted.size() + " 与要求的 " + expected.size() + " 次不符");
        }
        for (int i = 0; i < expected.size(); i++) {
            Trajectory.Point want = expected.get(i);
            Trajectory.Point got = submitted.get(i);
            double dist = Math.hypot(want.x() - got.x(), want.y() - got.y());
            if (dist > tolerance) {
                return BehaviorVerdict.reject(BehaviorVerdict.Reason.ANSWER_MISMATCH,
                        String.format("第 %d 次点击偏离 %.1fpx，超过容差 %dpx", i + 1, dist, tolerance));
            }
        }
        return BehaviorVerdict.pass();
    }
}
