package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * 拼图块轮廓。
 *
 * <h3>关键约束：形状必须严格落在画布内</h3>
 * <p>拼图块是"按形状裁剪出来的一小块图"，如果形状的包围盒超出画布，
 * 凸出的部分就会被画布边界裁掉，最终得到的是一个方块——
 * 而缺口却按完整形状绘制，两者形状对不上，用户永远对不准位置。
 *
 * <p>因此这里把<b>基础方块</b>往内缩进一个凸舌的尺寸，让凸舌正好顶到画布边缘：
 * <pre>
 *   ┌─────────────────┐  ← 画布 box×box
 *   │   ┌─────────┐   │
 *   │   │ 基础方块 │◗──│  ← 凸舌半径 = tab，正好触及右边界
 *   │   └─────────┘   │
 *   └─────────────────┘
 * </pre>
 *
 * @author created by lucky845
 */
public enum PuzzleShape {

    /**
     * 上方凸起
     */
    TAB_TOP(0, -1),

    /**
     * 右侧凸起
     */
    TAB_RIGHT(1, 0),

    /**
     * 下方凸起
     */
    TAB_BOTTOM(0, 1),

    /**
     * 左侧凸起
     */
    TAB_LEFT(-1, 0);

    /**
     * 凸舌相对画布的尺寸比例，取值 0.10~0.18 之间比较自然
     */
    private static final double TAB_RATIO = 0.16;

    /**
     * 凸舌最小半径
     */
    private static final int MIN_TAB = 3;

    private final int dirX;

    private final int dirY;

    PuzzleShape(int dirX, int dirY) {
        this.dirX = dirX;
        this.dirY = dirY;
    }

    /**
     * 计算指定画布尺寸下的凸舌半径。
     *
     * @param boxSize 画布边长
     * @return 凸舌半径
     */
    public static int tabSize(int boxSize) {
        return Math.max(MIN_TAB, (int) Math.round(boxSize * TAB_RATIO));
    }

    /**
     * 生成轮廓，保证形状的包围盒**严格等于** {@code [0, boxSize] × [0, boxSize]}。
     *
     * @param boxSize 画布边长
     * @return 形状
     */
    public Shape asShape(int boxSize) {
        int tab = tabSize(boxSize);
        int base = boxSize - 2 * tab;
        // 画布太小，退化成方块，避免出现负尺寸
        if (base < 6) {
            return new Rectangle2D.Double(0, 0, boxSize, boxSize);
        }

        // 基础方块内缩一个 tab，给凸舌留出空间
        double bx = tab;
        double by = tab;
        double bs = base;

        // 凸舌圆心落在基础方块某条边的中点，半径 tab，正好顶到画布边缘
        double cx = bx + bs / 2.0 + dirX * (bs / 2.0);
        double cy = by + bs / 2.0 + dirY * (bs / 2.0);

        Shape baseRect = new Rectangle2D.Double(bx, by, bs, bs);
        Shape tabShape = new Ellipse2D.Double(cx - tab, cy - tab, tab * 2.0, tab * 2.0);

        Area area = new Area(baseRect);
        area.add(new Area(tabShape));
        return area;
    }

    /**
     * 凸舌朝向，用于前端理解形状。
     */
    public String direction() {
        return switch (this) {
            case TAB_TOP -> "top";
            case TAB_RIGHT -> "right";
            case TAB_BOTTOM -> "bottom";
            case TAB_LEFT -> "left";
        };
    }

    /**
     * 随机取一种轮廓。
     */
    public static PuzzleShape random() {
        return RandomUtils.pick(values());
    }
}
