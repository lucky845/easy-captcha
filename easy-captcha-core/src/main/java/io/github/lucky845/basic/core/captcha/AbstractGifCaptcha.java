package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.encoder.AnimatedGifEncoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

import static io.github.lucky845.basic.core.utils.RandomUtils.num;

/**
 * GIF验证码抽象类
 *
 * @author created by lucky845 on 2025-01-16
 */
public abstract class AbstractGifCaptcha extends AbstractCaptcha {

    // 优化GIF动画参数
    private static final int FRAME_DELAY = 50;  // 减少帧延迟使动画更流畅
    private static final int FRAME_COUNT = 15;  // 增加帧数
    private static final float WAVE_FREQUENCY = 3.0f; // 波浪频率
    private static final float WAVE_AMPLITUDE = 0.4f; // 波浪振幅

    /**
     * 生成 GIF 验证码
     *
     * @param out 输出流
     * @return 是否成功
     */
    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        try {
            // 创建 AnimatedGifEncoder
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(out); // 初始化输出流
            encoder.setDelay(FRAME_DELAY); // 设置帧延迟
            encoder.setRepeat(0); // 设置循环次数（0 表示无限循环）

            // 生成 GIF 帧
            generateFrames(encoder);

            // 完成 GIF 生成
            encoder.finish();
            return true;
        } catch (Exception e) {
            throw new CaptchaGeneratorException("生成 GIF 验证码失败", e);
        }
    }

    /**
     * 计算波浪形透明度 - 优化波浪效果
     */
    protected float calculateAlpha(int x, int frameIndex, int imageWidth) {
        double phase = 2 * Math.PI * frameIndex / AbstractGifCaptcha.FRAME_COUNT;
        double wave = Math.sin(WAVE_FREQUENCY * 2 * Math.PI * (x + frameIndex * 8) / imageWidth + phase);
        return (float) (0.6 + WAVE_AMPLITUDE * wave); // 基础透明度0.6，波动范围更合理
    }

    /**
     * 生成 GIF 帧 - 优化动画效果
     */
    protected void generateFrames(AnimatedGifEncoder encoder) {
        String captchaText = getCaptcha();
        char[] charArray = captchaText.toCharArray();

        // 预计算字符位置和样式
        CharacterStyle[] charStyles = calculateCharacterStyles(charArray);

        // 生成动画帧
        for (int frameIndex = 0; frameIndex < FRAME_COUNT; frameIndex++) {
            BufferedImage frame = createFrame(charArray, charStyles, frameIndex);
            encoder.addFrame(frame);
        }
    }

    /**
     * 预计算每个字符的样式
     */
    private CharacterStyle[] calculateCharacterStyles(char[] charArray) {
        CharacterStyle[] styles = new CharacterStyle[charArray.length];
        FontMetrics metrics = new Canvas().getFontMetrics(font());
        int charWidth = metrics.charWidth('汉');
        int spacing = (captchaWidth - charWidth * charArray.length) / (charArray.length + 1);

        for (int i = 0; i < charArray.length; i++) {
            styles[i] = new CharacterStyle(
                    spacing + (spacing + charWidth) * i,  // x
                    captchaHeight / 2 + num(-10, 10),    // y
                    randomColor(),                       // color
                    num(-15, 15)                         // rotation
            );
        }
        return styles;
    }

    /**
     * 创建单个动画帧
     */
    private BufferedImage createFrame(char[] charArray, CharacterStyle[] styles, int frameIndex) {
        BufferedImage frame = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = frame.createGraphics();

        // 设置渲染品质
        configureGraphics(g2d);

        // 绘制背景和干扰元素
        drawBackground(g2d, frameIndex);

        // 绘制字符
        drawCharacters(g2d, charArray, styles, frameIndex);

        g2d.dispose();
        return frame;
    }

    /**
     * 配置绘图上下文
     */
    private void configureGraphics(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * 绘制背景和干扰元素
     */
    private void drawBackground(Graphics2D g2d, int frameIndex) {
        // 填充背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);

        // 动态干扰线
        drawBesselLine(2, new Color(0, 0, 0, 40), g2d);
        drawOval(3, new Color(0, 0, 0, 35), g2d);
    }

    /**
     * 绘制字符
     */
    private void drawCharacters(Graphics2D g2d, char[] charArray, CharacterStyle[] styles, int frameIndex) {
        g2d.setFont(font());

        for (int i = 0; i < charArray.length; i++) {
            CharacterStyle style = styles[i];
            float alpha = calculateAlpha(style.x, frameIndex, captchaWidth);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(style.color);

            // 应用旋转
            double rotation = style.rotation * Math.sin(2 * Math.PI * frameIndex / FRAME_COUNT);
            g2d.rotate(Math.toRadians(rotation), style.x, style.y);
            g2d.drawString(String.valueOf(charArray[i]), style.x, style.y);
            g2d.rotate(Math.toRadians(-rotation), style.x, style.y);
        }
    }

    /**
     * 字符样式内部类
     */
    private record CharacterStyle(int x, int y, Color color, double rotation) {
    }

    /**
     * 获取字体
     */
    protected abstract Font font();

}
