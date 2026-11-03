package io.github.lucky845.basic.core.behavior;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为轨迹校验测试。
 *
 * <h3>测试哲学：真人优先</h3>
 * <p>误杀真人比漏放脚本严重得多，所以本测试的重点是<b>覆盖各种真人操作形态</b>，
 * 确认它们全部通过；机器特征只测"明确的三类"。
 *
 * @author created by lucky845
 */
@DisplayName("行为轨迹校验")
class TrajectoryValidatorTest {

    private static final BehaviorTuning DRAG = BehaviorTuning.dragDefaults();

    private static final BehaviorTuning CLICK = BehaviorTuning.clickDefaults();

    /**
     * 构造拖动轨迹。抖动由 y 值模拟，时间均匀分布。
     */
    private static Trajectory dragPath(int distance, long totalMs, int steps) {
        List<Trajectory.Point> points = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            points.add(new Trajectory.Point(
                    (int) Math.round(distance * ratio),
                    (int) Math.round(Math.sin(i * 1.3) * 3),
                    Math.round(totalMs * ratio)));
        }
        return new Trajectory(points, totalMs);
    }

    /**
     * 构造完全笔直的拖动轨迹（真人常见操作）。
     */
    private static Trajectory straightPath(int distance, long totalMs) {
        List<Trajectory.Point> points = new ArrayList<>();
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            points.add(new Trajectory.Point(distance * i / steps, 0, totalMs * i / steps));
        }
        return new Trajectory(points, totalMs);
    }

    @Nested
    @DisplayName("真人拖动必须通过")
    class DragAcceptance {

        @ParameterizedTest(name = "耗时 {0}ms")
        @ValueSource(longs = {150, 200, 350, 500, 800, 1500, 3000, 10_000})
        @DisplayName("各种耗时的正常拖动都应通过")
        void anyReasonableDurationPasses(long duration) {
            BehaviorVerdict v = TrajectoryValidator.validateDrag(dragPath(200, duration, 20), DRAG);
            assertTrue(v.passed(), () -> "耗时 " + duration + "ms 的正常拖动被误杀：" + v.detail());
        }

        @Test
        @DisplayName("笔直拖动必须通过（真人常这么拖）")
        void straightDragPasses() {
            BehaviorVerdict v = TrajectoryValidator.validateDrag(straightPath(200, 800), DRAG);
            assertTrue(v.passed(), () -> "笔直拖动被误杀：" + v.detail());
        }

        @Test
        @DisplayName("只上报 1 个点也必须通过（低采样率设备）")
        void singlePointDragPasses() {
            Trajectory t = new Trajectory(
                    List.of(new Trajectory.Point(200, 0, 900)), 900);
            assertTrue(TrajectoryValidator.validateDrag(t, DRAG).passed());
        }

        @Test
        @DisplayName("拖过头再拉回来必须通过")
        void overshootAndPullBackPasses() {
            Trajectory t = new Trajectory(List.of(
                    new Trajectory.Point(0, 0, 0),
                    new Trajectory.Point(260, 2, 500),
                    new Trajectory.Point(200, 1, 900)), 900);
            BehaviorVerdict v = TrajectoryValidator.validateDrag(t, DRAG);
            assertTrue(v.passed(), () -> "拖过头回拉被误杀：" + v.detail());
        }

        @Test
        @DisplayName("拿不到轨迹也要通过（隐私模式）")
        void missingTrajectoryPasses() {
            assertTrue(TrajectoryValidator.validateDrag(null, DRAG).passed());
            assertTrue(TrajectoryValidator.validateDrag(new Trajectory(List.of(), 0), DRAG).passed());
        }
    }

    @Nested
    @DisplayName("真人点选必须通过")
    class ClickAcceptance {

        @ParameterizedTest(name = "耗时 {0}ms")
        @ValueSource(longs = {100, 150, 250, 400, 800, 2000, 8000})
        @DisplayName("各种手速的点选都应通过")
        void anyReasonableClickDurationPasses(long duration) {
            Trajectory t = new Trajectory(List.of(
                    new Trajectory.Point(40, 25, 0),
                    new Trajectory.Point(110, 30, duration / 2),
                    new Trajectory.Point(160, 22, duration)), duration);
            BehaviorVerdict v = TrajectoryValidator.validateClickBehavior(t, CLICK);
            assertTrue(v.passed(), () -> "耗时 " + duration + "ms 的点选被误杀：" + v.detail());
        }

        @Test
        @DisplayName("点选的坐标是折线，弯曲度天然接近 1，绝不能拿来判定")
        void clickCurvatureNeverUsed() {
            // 三个点几乎共线，弯曲度约为 1.0，但仍必须通过
            Trajectory collinear = new Trajectory(List.of(
                    new Trajectory.Point(10, 10, 0),
                    new Trajectory.Point(60, 10, 150),
                    new Trajectory.Point(110, 10, 300)), 300);
            assertTrue(collinear.curvature() < 1.02, "构造的轨迹应接近直线");
            assertTrue(TrajectoryValidator.validateClickBehavior(collinear, CLICK).passed(),
                    "点选不应因为轨迹笔直而被拒");
        }
    }

    @Nested
    @DisplayName("明确的机器特征必须拦截")
    class RobotRejection {

        @Test
        @DisplayName("耗时下限默认关闭：快速提交不算机器（回归：误杀快手用户）")
        void fastSubmissionIsNotRobot() {
            // 默认阈值不设下限，极快提交也必须放行——耗时主要由网络决定，
            // 与是否机器人无关，拿它做下限会误杀大量真实用户。
            assertTrue(TrajectoryValidator.validateDrag(dragPath(200, 5, 5), DRAG).passed());
            assertTrue(TrajectoryValidator.validateClickBehavior(
                    new Trajectory(List.of(new Trajectory.Point(10, 10, 0)), 1), CLICK).passed());
        }

        @Test
        @DisplayName("显式开启耗时下限后，低于阈值的提交被拦")
        void explicitMinDurationRejects() {
            BehaviorTuning strictTiming = BehaviorTuning.withMinDuration(500);
            BehaviorVerdict v = TrajectoryValidator.validateDrag(dragPath(200, 50, 20), strictTiming);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.TOO_FAST, v.reason());
        }

        @Test
        @DisplayName("拖动大幅横跳")
        void largeBackJumpRejected() {
            Trajectory t = new Trajectory(List.of(
                    new Trajectory.Point(0, 0, 0),
                    new Trajectory.Point(300, 0, 200),
                    new Trajectory.Point(10, 0, 400),
                    new Trajectory.Point(200, 0, 900)), 900);
            BehaviorVerdict v = TrajectoryValidator.validateDrag(t, DRAG);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.ABNORMAL_DIRECTION, v.reason());
        }

        @Test
        @DisplayName("操作超时")
        void timeoutRejected() {
            BehaviorVerdict v = TrajectoryValidator.validateDrag(
                    dragPath(200, 400_000, 20), DRAG);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.TIMEOUT, v.reason());
        }
    }

    @Nested
    @DisplayName("完整校验：答案 + 行为")
    class Combined {

        @Test
        @DisplayName("答案对且轨迹正常 → 通过")
        void sliderCorrectPasses() {
            BehaviorVerdict v = TrajectoryValidator.validateSlider(
                    100, 104, 8, dragPath(104, 800, 20), DRAG);
            assertTrue(v.passed(), () -> v.detail());
        }

        @Test
        @DisplayName("答案偏差超出容差 → 拒绝")
        void sliderWrongOffsetRejected() {
            BehaviorVerdict v = TrajectoryValidator.validateSlider(
                    100, 140, 8, dragPath(140, 800, 20), DRAG);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, v.reason());
        }

        @Test
        @DisplayName("轨迹异常时优先返回行为原因，不泄露答案对错（防探测）")
        void behaviorReasonTakesPrecedence() {
            // 构造一条大幅横跳的异常轨迹
            Trajectory abnormal = new Trajectory(List.of(
                    new Trajectory.Point(0, 0, 0),
                    new Trajectory.Point(300, 0, 300),
                    new Trajectory.Point(10, 0, 600),
                    new Trajectory.Point(200, 0, 900)), 900);

            // 答案错 + 轨迹异常 → 返回行为原因而非 ANSWER_MISMATCH
            BehaviorVerdict wrongAndAbnormal = TrajectoryValidator.validateSlider(
                    100, 999, 8, abnormal, DRAG);
            assertEquals(BehaviorVerdict.Reason.ABNORMAL_DIRECTION, wrongAndAbnormal.reason(),
                    "答案错误不应优先暴露，否则可被用来探测正确偏移量");

            // 答案对 + 轨迹异常 → 同样返回行为原因
            BehaviorVerdict rightAndAbnormal = TrajectoryValidator.validateSlider(
                    100, 100, 8, abnormal, DRAG);
            assertEquals(BehaviorVerdict.Reason.ABNORMAL_DIRECTION, rightAndAbnormal.reason());
        }

        @Test
        @DisplayName("点选顺序正确 → 通过")
        void clickCorrectPasses() {
            List<Trajectory.Point> expected = List.of(
                    new Trajectory.Point(50, 30, 0),
                    new Trajectory.Point(120, 40, 0),
                    new Trajectory.Point(80, 90, 0));
            List<Trajectory.Point> submitted = List.of(
                    new Trajectory.Point(53, 33, 0),
                    new Trajectory.Point(116, 44, 0),
                    new Trajectory.Point(84, 88, 0));
            BehaviorVerdict v = TrajectoryValidator.validateClick(
                    expected, submitted, 18, dragPath(100, 600, 10), CLICK);
            assertTrue(v.passed(), () -> v.detail());
        }

        @Test
        @DisplayName("点选顺序错误 → 拒绝")
        void clickWrongOrderRejected() {
            List<Trajectory.Point> expected = List.of(
                    new Trajectory.Point(50, 30, 0),
                    new Trajectory.Point(120, 40, 0));
            List<Trajectory.Point> submitted = List.of(
                    new Trajectory.Point(120, 40, 0),
                    new Trajectory.Point(50, 30, 0));
            BehaviorVerdict v = TrajectoryValidator.validateClick(
                    expected, submitted, 18, dragPath(100, 600, 10), CLICK);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, v.reason());
        }

        @Test
        @DisplayName("点选次数不符 → 拒绝")
        void clickWrongCountRejected() {
            BehaviorVerdict v = TrajectoryValidator.validateClick(
                    List.of(new Trajectory.Point(10, 10, 0)),
                    List.of(new Trajectory.Point(10, 10, 0), new Trajectory.Point(20, 20, 0)),
                    18, dragPath(100, 600, 10), CLICK);
            assertFalse(v.passed());
            assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, v.reason());
        }
    }

    @Nested
    @DisplayName("度量计算")
    class Metrics {

        @Test
        @DisplayName("直线轨迹弯曲度为 1")
        void straightCurvature() {
            List<Trajectory.Point> straight = List.of(
                    new Trajectory.Point(0, 0, 0),
                    new Trajectory.Point(50, 0, 100),
                    new Trajectory.Point(100, 0, 200));
            assertEquals(1.0, new Trajectory(straight, 200).curvature(), 0.001);
        }

        @Test
        @DisplayName("带抖动轨迹弯曲度大于 1")
        void wavyCurvature() {
            assertTrue(dragPath(100, 800, 20).curvature() > 1.0);
        }

        @Test
        @DisplayName("有效耗时取 duration 与轨迹时间戳的较大值")
        void effectiveDurationUsesMax() {
            Trajectory t = new Trajectory(List.of(
                    new Trajectory.Point(0, 0, 0),
                    new Trajectory.Point(100, 0, 900)), 200);
            assertEquals(900, t.effectiveDuration());
            assertTrue(t.hasTimingInfo());
        }

        @Test
        @DisplayName("完全没有时间信息时可识别")
        void noTimingInfo() {
            Trajectory t = new Trajectory(List.of(
                    new Trajectory.Point(0, 0, 0)), 0);
            assertFalse(t.hasTimingInfo());
        }
    }
}