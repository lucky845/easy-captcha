package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.encoder.AnimatedGifEncoder;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ColorEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.result.CaptchaResult;
import io.github.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * GIF 字符验证码生成器：逐帧做正弦波动，字符颜色固定，层次清晰。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class GifCaptchaGenerator extends AbstractCaptchaGenerator {

    private static final int FRAME_COUNT = 10;

    private static final int FRAME_DELAY_MS = 100;

    private static final double FONT_RATIO = 0.72;

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.GIF_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.GIF;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        String code = generateRandomText(config);
        Font font = font(config, FONT_RATIO);
        return new CaptchaResult(code, renderAnimated(config, code, font), contentType());
    }

    /**
     * 逐帧渲染动画 GIF。干扰线只生成一次，保证帧间不闪烁。
     */
    protected byte[] renderAnimated(CaptchaConfig config, String code, Font font) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        if (!encoder.start(out)) {
            throw new CaptchaGeneratorException("GIF 编码器启动失败");
        }
        encoder.setDelay(FRAME_DELAY_MS);
        encoder.setRepeat(0);
        encoder.setDispose(2);
        encoder.setQuality(10);

        int width = config.width();
        int height = config.height();
        int lineCount = DEFAULT_LINE_COUNT;
        int[] xs1 = new int[lineCount];
        int[] ys1 = new int[lineCount];
        int[] xs2 = new int[lineCount];
        int[] ys2 = new int[lineCount];
        Color[] lineColors = new Color[lineCount];
        for (int i = 0; i < lineCount; i++) {
            xs1[i] = RandomUtils.nextInt(0, width);
            ys1[i] = RandomUtils.nextInt(0, height);
            xs2[i] = RandomUtils.nextInt(0, width);
            ys2[i] = RandomUtils.nextInt(0, height);
            lineColors[i] = ColorEnum.randomLightColor();
        }

        Color[] charColors = new Color[code.length()];
        for (int i = 0; i < charColors.length; i++) {
            charColors[i] = ColorEnum.randomColor();
        }

        for (int frameIndex = 0; frameIndex < FRAME_COUNT; frameIndex++) {
            BufferedImage frame = newArgbImage(config);
            Graphics2D g = frame.createGraphics();
            try {
                applyQualityHints(g);
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < lineCount; i++) {
                    g.setColor(lineColors[i]);
                    g.drawLine(xs1[i], ys1[i], xs2[i], ys2[i]);
                }
                drawFrameText(g, code, config, font, charColors, frameIndex);
            } finally {
                g.dispose();
            }
            if (!encoder.addFrame(frame)) {
                throw new CaptchaGeneratorException("GIF 帧写入失败");
            }
        }
        if (!encoder.finish()) {
            throw new CaptchaGeneratorException("GIF 编码收尾失败");
        }
        return out.toByteArray();
    }

    /**
     * 在当前帧上绘制文字，逐字符做正弦波动。
     */
    private void drawFrameText(Graphics2D g, String code, CaptchaConfig config,
                               Font font, Color[] colors, int frameIndex) {
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int count = code.length();
        int cellWidth = Math.max(1, config.width() / count);
        int baseline = (config.height() - metrics.getHeight()) / 2 + metrics.getAscent();
        int amplitude = Math.max(1, Math.min(4, config.height() / 12));
        double phase = 2 * Math.PI * frameIndex / FRAME_COUNT;

        for (int i = 0; i < count; i++) {
            char ch = code.charAt(i);
            int charWidth = Math.max(1, metrics.charWidth(ch));
            int x = i * cellWidth + Math.max(0, (cellWidth - charWidth) / 2);
            double wave = Math.sin(phase + i * 0.7);
            int y = baseline + (int) Math.round(wave * amplitude);
            double angle = Math.toRadians(wave * 10);
            g.setColor(colors[i]);
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
}
