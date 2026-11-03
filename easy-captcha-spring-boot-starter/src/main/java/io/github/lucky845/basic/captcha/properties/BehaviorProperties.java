package io.github.lucky845.basic.captcha.properties;

import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 行为验证的轨迹判定阈值。
 *
 * <h3>默认值全部是"宽容档"</h3>
 * <p>判定原则是<b>宁可漏放，不可误杀</b>：误杀会让真实用户直接流失，
 * 而漏放的脚本还有限流、一次性销毁、二次票据三层兜底。
 * 因此所有"能不能通过"的判定默认都关得很松。
 *
 * <h3>什么时候该调严</h3>
 * <ul>
 *     <li>接口正在被批量刷：先把 {@code easy-captcha.rate-limit-permits} 降下来，
 *         比收紧轨迹有效得多；</li>
 *     <li>确认自家用户网络延迟稳定（比如纯内网）：才能考虑设置 {@code min-duration-ms}；</li>
 *     <li>高风险动作（提现、改密码）：打开 {@code check-curvature} 并要求更多轨迹点。</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * easy-captcha:
 *   slider-tolerance: 8          # 滑块偏移量允许误差
 *   click-tolerance: 18          # 点选每次点击允许误差
 *   behavior:
 *     drag:
 *       min-duration-ms: 0       # 0 = 不设下限（推荐）
 *       max-duration-ms: 300000
 *       min-points: 1            # 1 = 不限制
 *       monotonic-tolerance: 60  # 允许拖过头 60px 再拉回
 *       check-curvature: false
 *     click:
 *       min-duration-ms: 0
 *       max-duration-ms: 300000
 * </pre>
 *
 * @author created by lucky845
 */
public class BehaviorProperties {

    /**
     * 拖动型（滑块）阈值
     */
    @NestedConfigurationProperty
    private Drag drag = new Drag();

    /**
     * 点击型（点选）阈值
     */
    @NestedConfigurationProperty
    private Click click = new Click();

    public Drag getDrag() {
        return drag;
    }

    public void setDrag(Drag drag) {
        this.drag = drag;
    }

    public Click getClick() {
        return click;
    }

    public void setClick(Click click) {
        this.click = click;
    }

    /**
     * 拖动型阈值对象，供自动配置直接使用。
     */
    public BehaviorTuning dragTuning() {
        return drag.toTuning();
    }

    /**
     * 点击型阈值对象，供自动配置直接使用。
     */
    public BehaviorTuning clickTuning() {
        return click.toTuning();
    }

    /**
     * 拖动型（滑块）阈值。
     */
    public static class Drag {

        /**
         * 最短耗时（毫秒）。默认 0 = <b>不设下限</b>。
         *
         * <p>从服务端签发到提交的时间主要由网络决定，与是否机器人无关，
         * 用它做下限会误杀快手用户与内网部署。需要时再显式设值。
         */
        private long minDurationMs = 0;

        /**
         * 最长耗时（毫秒）。默认 5 分钟，用于拦截"挂机很久再重放"。
         */
        private long maxDurationMs = 300_000;

        /**
         * 最少轨迹点数。默认 1 = 不限制（低采样率设备常常只上报终端点）。
         */
        private int minPoints = 1;

        /**
         * 允许的 X 方向回退像素。默认 60，允许用户拖过头再拉回来。
         */
        private int monotonicTolerance = 60;

        /**
         * 是否启用弯曲度检查。默认 false：真人拖直线是常态。
         */
        private boolean checkCurvature = false;

        /**
         * 弯曲度下限，仅在 {@code check-curvature=true} 时生效。
         */
        private double minCurvature = 1.005;

        public long getMinDurationMs() {
            return minDurationMs;
        }

        public void setMinDurationMs(long minDurationMs) {
            this.minDurationMs = minDurationMs;
        }

        public long getMaxDurationMs() {
            return maxDurationMs;
        }

        public void setMaxDurationMs(long maxDurationMs) {
            this.maxDurationMs = maxDurationMs;
        }

        public int getMinPoints() {
            return minPoints;
        }

        public void setMinPoints(int minPoints) {
            this.minPoints = minPoints;
        }

        public int getMonotonicTolerance() {
            return monotonicTolerance;
        }

        public void setMonotonicTolerance(int monotonicTolerance) {
            this.monotonicTolerance = monotonicTolerance;
        }

        public boolean isCheckCurvature() {
            return checkCurvature;
        }

        public void setCheckCurvature(boolean checkCurvature) {
            this.checkCurvature = checkCurvature;
        }

        public double getMinCurvature() {
            return minCurvature;
        }

        public void setMinCurvature(double minCurvature) {
            this.minCurvature = minCurvature;
        }

        /**
         * 转换为校验器使用的阈值对象，非法值夹到安全范围。
         */
        public BehaviorTuning toTuning() {
            return new BehaviorTuning(
                    Math.max(0, minDurationMs),
                    maxDurationMs,
                    Math.max(1, minPoints),
                    Math.max(0, monotonicTolerance),
                    checkCurvature,
                    Math.max(1.0, minCurvature));
        }
    }

    /**
     * 点击型（点选）阈值。
     */
    public static class Click {

        /**
         * 最短耗时（毫秒）。默认 0 = 不设下限。
         */
        private long minDurationMs = 0;

        /**
         * 最长耗时（毫秒）。
         */
        private long maxDurationMs = 300_000;

        public long getMinDurationMs() {
            return minDurationMs;
        }

        public void setMinDurationMs(long minDurationMs) {
            this.minDurationMs = minDurationMs;
        }

        public long getMaxDurationMs() {
            return maxDurationMs;
        }

        public void setMaxDurationMs(long maxDurationMs) {
            this.maxDurationMs = maxDurationMs;
        }

        /**
         * 转换为校验器使用的阈值对象。
         *
         * <p>点选只有离散坐标，点数、方向、弯曲度都无意义，因此固定不参与判定。
         */
        public BehaviorTuning toTuning() {
            return new BehaviorTuning(
                    Math.max(0, minDurationMs),
                    maxDurationMs,
                    1,
                    Integer.MAX_VALUE,
                    false,
                    1.0);
        }
    }
}
