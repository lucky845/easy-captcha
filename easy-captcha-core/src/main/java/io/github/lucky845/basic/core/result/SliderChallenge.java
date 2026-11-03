package io.github.lucky845.basic.core.result;

import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑块验证码的前端渲染数据。
 *
 * <p>注意：本对象会被序列化后发给浏览器，因此**只放可以公开的内容**：
 * 背景图、拼图块小图、拼图块的尺寸与纵向位置。缺口究竟在水平方向的什么位置
 * 属于答案，绝不能出现在这里。
 *
 * @param background  背景图（含缺口）的 data URI
 * @param block       拼图块小图的 data URI
 * @param blockWidth  拼图块宽度（px）
 * @param blockHeight 拼图块高度（px）
 * @param blockY      拼图块初始纵坐标（px），横向初始位置固定为 0
 * @param imageWidth  背景图宽度
 * @param imageHeight 背景图高度
 * @author created by lucky845
 */
public record SliderChallenge(
        String background,
        String block,
        int blockWidth,
        int blockHeight,
        int blockY,
        int imageWidth,
        int imageHeight
) {

    public SliderChallenge {
        if (background == null || block == null) {
            throw new CaptchaGeneratorException("滑块验证码的图片数据缺失");
        }
        if (blockWidth <= 0 || blockHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
            throw new CaptchaGeneratorException("滑块验证码的尺寸数据非法");
        }
    }

    /**
     * 转成 map，便于直接放进 JSON 响应。
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("background", background);
        map.put("block", block);
        map.put("blockWidth", blockWidth);
        map.put("blockHeight", blockHeight);
        map.put("blockY", blockY);
        map.put("imageWidth", imageWidth);
        map.put("imageHeight", imageHeight);
        return map;
    }
}
