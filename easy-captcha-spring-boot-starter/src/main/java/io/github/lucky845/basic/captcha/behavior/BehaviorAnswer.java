package io.github.lucky845.basic.captcha.behavior;

import java.util.List;

/**
 * 前端提交的行为验证码应答。
 *
 * <p>一次提交同时携带三样东西：
 * <ul>
 *     <li>{@code answer}：静态答案（滑块偏移量 / 点选坐标串）；</li>
 *     <li>{@code points}：鼠标/触摸轨迹，用于判断是不是人；</li>
 *     <li>{@code duration}：从开始操作到松手的耗时（毫秒）。</li>
 * </ul>
 *
 * @param answer   静态答案，滑块为偏移量字符串，点选为 "x,y;x,y" 形式
 * @param points   轨迹点
 * @param duration 耗时（毫秒）
 * @author created by lucky845
 */
public record BehaviorAnswer(String answer, List<Point> points, long duration) {

    public BehaviorAnswer {
        points = points == null ? List.of() : List.copyOf(points);
        if (duration < 0) {
            duration = 0;
        }
    }

    /**
     * 只有答案、没有轨迹的简易提交。
     */
    public static BehaviorAnswer of(String answer) {
        return new BehaviorAnswer(answer, List.of(), 0);
    }

    /**
     * 轨迹点。
     *
     * @param x 横坐标（相对验证码图片左上角）
     * @param y 纵坐标
     * @param t 相对开始时间的毫秒偏移
     */
    public record Point(int x, int y, long t) {
    }
}
