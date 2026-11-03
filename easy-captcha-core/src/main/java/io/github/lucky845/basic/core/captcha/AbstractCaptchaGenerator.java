package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.constants.ArithmeticTypeConstants;
import io.github.lucky845.basic.core.constants.Constants;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.ColorEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.utils.FontCache;
import io.github.lucky845.basic.core.utils.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 验证码生成器抽象基类，集中提供无状态的绘制工具。
 *
 * <p>本类不持有任何可变字段，所有随机性都在方法内部产生，
 * 因此子类实例可以被并发复用。
 *
 * @author created by lucky845 on 2025-01-16
 */
public abstract class AbstractCaptchaGenerator implements CaptchaGenerator {

    /**
     * 纯数字字符集
     */
    protected static final char[] NUMBER_CHARS =
            Arrays.copyOfRange(Constants.CHARS, 0, Constants.NUM_MAX_INDEX);

    /**
     * 全部大写字母
     */
    protected static final char[] UPPER_CHARS =
            Arrays.copyOfRange(Constants.CHARS, Constants.UPPER_MIN_INDEX, Constants.UPPER_MAX_INDEX);

    /**
     * 全部小写字母
     */
    protected static final char[] LOWER_CHARS =
            Arrays.copyOfRange(Constants.CHARS, Constants.LOWER_MIN_INDEX, Constants.LOWER_MAX_INDEX);

    /**
     * 全部字母（大写 + 小写）
     */
    protected static final char[] LETTER_CHARS =
            Arrays.copyOfRange(Constants.CHARS, Constants.CHAR_MIN_INDEX, Constants.CHAR_MAX_INDEX);

    /**
     * 数字 + 字母
     */
    protected static final char[] ALPHANUMERIC_CHARS = Constants.CHARS;

    /**
     * 干扰线默认数量
     */
    protected static final int DEFAULT_LINE_COUNT = 4;

    /**
     * 干扰圆默认数量
     */
    protected static final int DEFAULT_OVAL_COUNT = 4;

    /**
     * 干扰贝塞尔曲线默认数量
     */
    protected static final int DEFAULT_BEZIER_COUNT = 2;

    /**
     * 生成随机字符验证码文本。
     *
     * @param config 生成配置
     * @return 随机字符串
     */
    protected String generateRandomText(CaptchaConfig config) {
        StringBuilder sb = new StringBuilder(config.length());
        for (int i = 0; i < config.length(); i++) {
            sb.append(randomChar(config.charType()));
        }
        return sb.toString();
    }

    /**
     * 生成随机中文验证码文本。
     *
     * @param config 生成配置
     * @return 中文字符串
     */
    protected String generateChineseText(CaptchaConfig config) {
        StringBuilder sb = new StringBuilder(config.length());
        for (int i = 0; i < config.length(); i++) {
            sb.append(Constants.DELTA.charAt(RandomUtils.nextInt(Constants.DELTA.length())));
        }
        return sb.toString();
    }

    /**
     * 按字符类型取一个随机字符。
     */
    protected char randomChar(CharTypeEnum charType) {
        return switch (charType) {
            case PURE_NUMBER -> RandomUtils.randomChar(NUMBER_CHARS);
            case PURE_LETTERS -> RandomUtils.randomChar(LETTER_CHARS);
            case PURE_CAPITAL_LETTERS -> RandomUtils.randomChar(UPPER_CHARS);
            case PURE_LOWERCASE_LETTERS -> RandomUtils.randomChar(LOWER_CHARS);
            case ALPHANUMERIC_MIX -> RandomUtils.randomChar(ALPHANUMERIC_CHARS);
            case DEFAULT -> RandomUtils.randomChar(ALPHANUMERIC_CHARS);
        };
    }

    /**
     * 创建白底画布。
     */
    protected BufferedImage newImage(CaptchaConfig config) {
        BufferedImage image = new BufferedImage(
                config.width(), config.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, config.width(), config.height());
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 创建带透明通道的画布，用于 GIF 帧。
     */
    protected BufferedImage newArgbImage(CaptchaConfig config) {
        BufferedImage image = new BufferedImage(
                config.width(), config.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, config.width(), config.height());
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 开启抗锯齿等渲染质量选项。
     */
    protected void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * 编码为 PNG 字节数组。
     */
    protected byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            if (!ImageIO.write(image, "png", out)) {
                throw new CaptchaGeneratorException("没有可用的 PNG 编码器");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new CaptchaGeneratorException("PNG 编码失败", e);
        }
    }

    /**
     * 绘制默认干扰元素（浅色，保证字符可读）。
     */
    protected void drawInterference(Graphics2D g, CaptchaConfig config) {
        drawInterferenceLines(g, config, DEFAULT_LINE_COUNT);
        drawInterferenceOvals(g, config, DEFAULT_OVAL_COUNT);
        drawBezierLines(g, config, DEFAULT_BEZIER_COUNT);
    }

    /**
     * 绘制浅色干扰线。
     */
    protected void drawInterferenceLines(Graphics2D g, CaptchaConfig config, int count) {
        int width = config.width();
        int height = config.height();
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count; i++) {
            g.setColor(ColorEnum.randomLightColor());
            int x1 = RandomUtils.nextInt(0, width);
            int y1 = RandomUtils.nextInt(0, height);
            int x2 = RandomUtils.nextInt(0, width);
            int y2 = RandomUtils.nextInt(0, height);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制浅色干扰圆。
     */
    protected void drawInterferenceOvals(Graphics2D g, CaptchaConfig config, int count) {
        int width = config.width();
        int height = config.height();
        for (int i = 0; i < count; i++) {
            g.setColor(ColorEnum.randomLightColor());
            int w = RandomUtils.nextInt(6, Math.max(8, width / 8));
            int h = RandomUtils.nextInt(6, Math.max(8, height / 3));
            int x = RandomUtils.nextInt(0, Math.max(1, width - w));
            int y = RandomUtils.nextInt(0, Math.max(1, height - h));
            g.drawOval(x, y, w, h);
        }
    }

    /**
     * 绘制浅色贝塞尔曲线干扰。
     */
    protected void drawBezierLines(Graphics2D g, CaptchaConfig config, int count) {
        int width = config.width();
        int height = config.height();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count; i++) {
            g.setColor(ColorEnum.randomLightColor());
            int x1 = 2;
            int y1 = RandomUtils.nextInt(2, Math.max(3, height - 2));
            int x2 = width - 2;
            int y2 = RandomUtils.nextInt(2, Math.max(3, height - 2));
            int ctrlX = RandomUtils.nextInt(width / 4, Math.max(width / 4 + 1, width / 4 * 3));
            int ctrlY = RandomUtils.nextInt(2, Math.max(3, height - 2));
            if (RandomUtils.chance(0.5)) {
                QuadCurve2D curve = new QuadCurve2D.Double();
                curve.setCurve(x1, y1, ctrlX, ctrlY, x2, y2);
                g.draw(curve);
            } else {
                int ctrlX2 = RandomUtils.nextInt(width / 4, Math.max(width / 4 + 1, width / 4 * 3));
                int ctrlY2 = RandomUtils.nextInt(2, Math.max(3, height - 2));
                CubicCurve2D curve = new CubicCurve2D.Double(x1, y1, ctrlX, ctrlY, ctrlX2, ctrlY2, x2, y2);
                g.draw(curve);
            }
        }
    }

    /**
     * 把字符串逐字符均匀铺满画布，每个字符随机旋转、随机纵向偏移。
     *
     * @param g          画笔
     * @param text       要绘制的文本
     * @param config     配置
     * @param font       字体
     * @param randomize  是否对每个字符做随机扰动
     * @param maxRotation 最大旋转角度（度）
     */
    protected void drawDistributedChars(Graphics2D g, String text, CaptchaConfig config,
                                        Font font, boolean randomize, double maxRotation) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int width = config.width();
        int height = config.height();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int count = text.length();
        int cellWidth = Math.max(1, width / count);
        int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();

        for (int i = 0; i < count; i++) {
            char ch = text.charAt(i);
            int charWidth = Math.max(1, metrics.charWidth(ch));
            int cellStart = i * cellWidth;
            int x = cellStart + Math.max(0, (cellWidth - charWidth) / 2);
            int y = baseline;
            double angle = 0;
            if (randomize) {
                int maxDy = Math.max(1, height / 12);
                y += RandomUtils.nextInt(-maxDy, maxDy + 1);
                angle = Math.toRadians(RandomUtils.nextIntInclusive(
                        (int) -maxRotation, (int) maxRotation));
            }
            g.setColor(ColorEnum.randomColor());
            double pivotX = x + charWidth / 2.0;
            double pivotY = y - metrics.getAscent() / 2.0;
            if (angle != 0) {
                g.rotate(angle, pivotX, pivotY);
            }
            g.drawString(String.valueOf(ch), x, y);
            if (angle != 0) {
                g.rotate(-angle, pivotX, pivotY);
            }
        }
    }

    /**
     * 把整段文本水平垂直居中绘制。
     */
    protected void drawCenteredText(Graphics2D g, String text, CaptchaConfig config, Font font) {
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int x = Math.max(0, (config.width() - textWidth) / 2);
        int y = (config.height() - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(Color.BLACK);
        g.drawString(text, x, y);
    }

    /**
     * 生成两个操作数之间的随机算术运算符。
     */
    protected char randomArithmeticOperator() {
        return RandomUtils.pick(new Character[]{
                ArithmeticTypeConstants.ADDITION,
                ArithmeticTypeConstants.SUBTRACTION,
                ArithmeticTypeConstants.MULTIPLICATION,
                ArithmeticTypeConstants.DIVISION
        });
    }

    /**
     * 把字体收缩到文本能放进画布（保留两侧各 5px 边距）。
     */
    protected Font fitFont(Graphics2D g, String text, CaptchaConfig config, Font base) {
        int maxWidth = Math.max(10, config.width() - 10);
        Font candidate = base;
        float size = base.getSize2D();
        FontMetrics metrics = g.getFontMetrics(candidate);
        while (metrics.stringWidth(text) > maxWidth && size > 8f) {
            size -= 1f;
            candidate = base.deriveFont(size);
            metrics = g.getFontMetrics(candidate);
        }
        return candidate;
    }

    /**
     * 按配置加载内置字体。
     */
    protected Font font(CaptchaConfig config, double ratio) {
        return FontCache.load(config.font(), Font.BOLD, config.effectiveFontSize(ratio));
    }

    /**
     * 按配置加载中文字体。
     */
    protected Font cjkFont(CaptchaConfig config, double ratio) {
        return FontCache.cjkFont(Font.BOLD, config.effectiveFontSize(ratio));
    }
}
