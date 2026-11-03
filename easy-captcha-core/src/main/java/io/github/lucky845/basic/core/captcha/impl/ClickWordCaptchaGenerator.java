package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.behavior.Trajectory;
import io.github.lucky845.basic.core.captcha.AbstractCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.constants.Constants;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ColorEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import io.github.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文字点选验证码生成器。
 *
 * <p>与"看图输入"不同，点选验证码要求用户**按提示顺序点击**图上的若干汉字。
 * 抗打码能力来自两点：
 * <ul>
 *     <li>答案是一组坐标，OCR 认出文字也没用，还得知道服务端要求的顺序；</li>
 *     <li>点击过程本身留下轨迹，配合行为校验进一步过滤脚本。</li>
 * </ul>
 *
 * <p>答案 {@code code} 形如 {@code "12,34;156,22"}，即按顺序的点击坐标。
 *
 * @author created by lucky845
 */
public class ClickWordCaptchaGenerator extends AbstractCaptchaGenerator {

    /**
     * 图上总的候选字数量（包含干扰字）
     */
    private static final int CANDIDATE_COUNT = 6;

    /**
     * 要求用户点击的字数
     */
    private static final int TARGET_COUNT = 3;

    private static final double FONT_RATIO = 0.42;

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.CLICK_WORD_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        int width = config.width();
        int height = config.height();

        // 候选字：至少能放下 6 个，否则退回 4 个
        int count = Math.min(CANDIDATE_COUNT, Math.max(3, width / 34));
        count = Math.max(count, TARGET_COUNT);

        BufferedImage image = newImage(config);
        Graphics2D g = image.createGraphics();
        List<PlacedChar> placed;
        try {
            applyQualityHints(g);
            drawInterference(g, config);
            placed = placeChars(g, config, count);
        } finally {
            g.dispose();
        }

        // 随机挑 targetCount 个作为要点的字，顺序即答案顺序
        List<PlacedChar> shuffled = new ArrayList<>(placed);
        java.util.Collections.shuffle(shuffled, new java.util.Random());
        List<PlacedChar> targets = shuffled.subList(0, Math.min(TARGET_COUNT, shuffled.size()));

        StringBuilder code = new StringBuilder();
        List<Map<String, Object>> answerPoints = new ArrayList<>();
        StringBuilder prompt = new StringBuilder();
        for (int i = 0; i < targets.size(); i++) {
            PlacedChar target = targets.get(i);
            if (i > 0) {
                code.append(';');
            }
            code.append(target.centerX()).append(',').append(target.centerY());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("x", target.centerX());
            point.put("y", target.centerY());
            answerPoints.add(point);
            prompt.append(target.ch());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("prompt", prompt.toString());
        payload.put("targetCount", targets.size());
        payload.put("imageWidth", width);
        payload.put("imageHeight", height);
        payload.put("captchaType", CaptchaTypeEnum.CLICK_WORD_CAPTCHA);

        return new CaptchaResult(code.toString(), encodePng(image), contentType(), payload);
    }

    /**
     * 在画布上随机摆放互不重叠的字符。
     */
    private List<PlacedChar> placeChars(Graphics2D g, CaptchaConfig config, int count) {
        int width = config.width();
        int height = config.height();
        Font font = cjkFont(config, FONT_RATIO);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();

        int charW = Math.max(1, metrics.charWidth('汉'));
        int charH = Math.max(1, metrics.getHeight());
        // 每个字占据的格子
        int cellW = Math.max(charW + 6, width / count);
        int cellH = Math.max(charH + 4, height);

        List<PlacedChar> result = new ArrayList<>();
        List<Rectangle> occupied = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            char ch = Constants.DELTA.charAt(RandomUtils.nextInt(Constants.DELTA.length()));
            Rectangle box = null;
            int cx = 0;
            int cy = 0;
            // 最多尝试若干次，找一个不与已摆放字符重叠的位置
            for (int attempt = 0; attempt < 40; attempt++) {
                int cellStart = i * (width / count);
                int x = cellStart + RandomUtils.nextInt(0, Math.max(1, (width / count) - charW));
                int y = RandomUtils.nextInt(metrics.getAscent(), Math.max(metrics.getAscent() + 1,
                        height - metrics.getDescent()));
                box = new Rectangle(x - 3, y - metrics.getAscent() - 2, charW + 6, charH + 4);
                cx = x + charW / 2;
                cy = y - metrics.getAscent() / 2;
                if (box.x < 0 || box.y < 0 || box.x + box.width > width || box.y + box.height > height) {
                    continue;
                }
                boolean overlapped = occupied.stream().anyMatch(box::intersects);
                if (!overlapped) {
                    break;
                }
            }
            if (box == null) {
                continue;
            }
            occupied.add(box);

            double angle = Math.toRadians(RandomUtils.nextIntInclusive(-12, 12));
            g.setColor(ColorEnum.randomColor());
            double pivotX = box.x + box.width / 2.0;
            double pivotY = box.y + box.height / 2.0;
            if (angle != 0) {
                g.rotate(angle, pivotX, pivotY);
            }
            g.drawString(String.valueOf(ch), box.x + 3, box.y + box.height - 2);
            if (angle != 0) {
                g.rotate(-angle, pivotX, pivotY);
            }

            result.add(new PlacedChar(ch, box, cx, cy));
        }
        return result;
    }

    /**
     * 已摆放的字符。
     *
     * @param ch      字符
     * @param box     外接矩形
     * @param centerX 中心 X（作为点击目标）
     * @param centerY 中心 Y（作为点击目标）
     */
    private record PlacedChar(char ch, Rectangle box, int centerX, int centerY) {
    }
}
