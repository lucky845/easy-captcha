package io.github.lucky845.basic.core.behavior;

import java.util.List;
import java.util.Objects;

/**
 * 用户操作轨迹。
 *
 * <p>行为验证码的核心不是"答案对不对"这么简单，而是**人做这件事的轨迹
 * 长什么样**。机器通常表现为：瞬时完成、匀速直线、零抖动。
 * 因此轨迹校验要同时看终点、耗时与形状。
 *
 * @param points   轨迹点，按时间先后排列
 * @param duration 从开始操作到提交的耗时（毫秒）
 * @author created by lucky845
 */
public record Trajectory(List<Point> points, long duration) {

    public Trajectory {
        points = points == null ? List.of() : List.copyOf(points);
        if (duration < 0) {
            duration = 0;
        }
    }

    /**
     * 只有终点、没有中间点的"轨迹"。
     */
    public static Trajectory ofEndpoint(Point end, long duration) {
        return new Trajectory(List.of(end), duration);
    }

    /**
     * 轨迹点数量。
     */
    public int size() {
        return points.size();
    }

    /**
     * 最后一个点，没有轨迹时返回 {@code null}。
     */
    public Point last() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    /**
     * 第一个点，没有轨迹时返回 {@code null}。
     */
    public Point first() {
        return points.isEmpty() ? null : points.get(0);
    }

    /**
     * 是否包含足够的采样点。
     *
     * @param minPoints 最少点数
     */
    public boolean hasEnoughPoints(int minPoints) {
        return points.size() >= minPoints;
    }

    /**
     * 轨迹在 X 方向是否单调递增（允许小幅回退）。
     *
     * <p>人拖滑块时会有轻微来回修正，但整体方向必须向前；
     * 一条完全没有回退的直线往往意味着拖拽是由脚本一次性生成的。
     *
     * @param tolerance 允许的回退像素
     * @return 是否单调不减
     */
    public boolean xMonotonic(int tolerance) {
        if (points.size() < 2) {
            return true;
        }
        int previous = points.get(0).x();
        for (int i = 1; i < points.size(); i++) {
            int current = points.get(i).x();
            if (current < previous - tolerance) {
                return false;
            }
            previous = Math.max(previous, current);
        }
        return true;
    }

    /**
     * 轨迹总路程（逐点曼哈顿距离之和）。
     */
    public double totalDistance() {
        double sum = 0;
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1);
            Point b = points.get(i);
            sum += Math.hypot(b.x() - a.x(), b.y() - a.y());
        }
        return sum;
    }

    /**
     * 位移直线距离（起点到终点）。
     */
    public double straightDistance() {
        Point a = first();
        Point b = last();
        if (a == null || b == null) {
            return 0;
        }
        return Math.hypot(b.x() - a.x(), b.y() - a.y());
    }

    /**
     * 轨迹弯曲度：总路程 / 直线距离。完全笔直时为 1。
     */
    public double curvature() {
        double straight = straightDistance();
        if (straight < 1e-6) {
            return 1;
        }
        return totalDistance() / straight;
    }

    /**
     * 有效耗时。
     *
     * <p>前端有时拿不到 duration（传 0），但轨迹点自带的 {@code t} 仍然能反映真实耗时。
     * 取两者最大值，避免因为字段缺失而放行一个真正的脚本。
     *
     * @return 毫秒
     */
    public long effectiveDuration() {
        long maxT = 0;
        for (Point point : points) {
            if (point.t() > maxT) {
                maxT = point.t();
            }
        }
        return Math.max(duration, maxT);
    }

    /**
     * 是否含任何时间信息（duration 或轨迹点的时间戳）。
     */
    public boolean hasTimingInfo() {
        return effectiveDuration() > 0;
    }

    /**
     * 轨迹点。
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @param t 相对开始时间的毫秒偏移
     */
    public record Point(int x, int y, long t) {

        public Point {
            Objects.requireNonNull(x, "x");
        }
    }
}
