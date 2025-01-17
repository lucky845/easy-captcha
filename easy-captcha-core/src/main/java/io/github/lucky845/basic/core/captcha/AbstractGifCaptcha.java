package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.utils.GifSequenceWriter;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GIF验证码抽象类
 *
 * @author created by lucky845 on 2025-01-16
 */
public abstract class AbstractGifCaptcha extends AbstractCaptcha {

    // GIF 帧之间的延迟时间（单位：毫秒）
    private static final int FRAME_DELAY = 100;

    // GIF 帧列表
    private final List<BufferedImage> frames;

    public AbstractGifCaptcha() {
        this.frames = new ArrayList<>();
    }

    /**
     * 生成 GIF 验证码
     *
     * @param out 输出流
     * @return 是否成功
     */
    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        try {
            // 生成 GIF 帧
            generateFrames();

            // 使用 GifSequenceWriter 将帧写入输出流
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            GifSequenceWriter writer = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_RGB, FRAME_DELAY, true);
            for (BufferedImage frame : frames) {
                writer.writeToSequence(frame);
            }
            writer.close();

            return true;
        } catch (IOException e) {
            throw new CaptchaGeneratorException("生成 GIF 验证码失败", e);
        }
    }

    /**
     * 获取图片类型
     *
     * @return 图片格式，MIME类型
     */
    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.GIF;
    }

    /**
     * 生成 GIF 帧
     */
    protected abstract void generateFrames();

    /**
     * 添加一帧到 GIF
     *
     * @param frame 帧图像
     */
    protected void addFrame(BufferedImage frame) {
        frames.add(frame);
    }

    /**
     * 清空帧列表
     */
    protected void clearFrames() {
        frames.clear();
    }

    /**
     * 获取当前帧列表
     *
     * @return 帧列表
     */
    protected List<BufferedImage> getFrames() {
        return frames;
    }
}
