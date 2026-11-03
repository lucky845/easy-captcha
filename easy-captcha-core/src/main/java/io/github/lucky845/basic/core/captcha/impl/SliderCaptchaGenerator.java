package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.PuzzleShape;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.result.CaptchaResult;
import io.github.lucky845.basic.core.result.SliderChallenge;
import io.github.lucky845.basic.core.utils.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 滑块拼图验证码生成器。
 *
 * <h3>生成流程</h3>
 * <ol>
 *     <li>取底图：优先用调用方提供的自定义底图，否则用内置渐变底图；</li>
 *     <li>在干净底图上按拼图形状抠出滑块（形状与缺口严格一致）；</li>
 *     <li>在底图上绘制缺口 + 若干<b>假缺口</b>作为干扰；</li>
 *     <li>返回缺口横向位置作为答案。</li>
 * </ol>
 *
 * <h3>为什么要有假缺口</h3>
 * <p>只有真缺口时，脚本只要做一次模板匹配就能定位目标，成本极低。
 * 加上假缺口后，候选位置变多，必须结合"形状朝向"与"边缘连续"才能辨别，
 * 而真人一眼就能看出哪个缺口是"真的"（因为只有真缺口配了可拖动的滑块）。
 *
 * @author created by lucky845
 */
public class SliderCaptchaGenerator extends AbstractCaptchaGenerator {

    private static final Logger LOG = Logger.getLogger(SliderCaptchaGenerator.class.getName());

    /**
     * 拼图块边长占图片短边的比例。
     *
     * <p>取 0.42 而不是更小的值，是因为凸舌只有块边长的 16%：
     * 块越小，凸舌越看不清，用户越难判断"我到底对准了没有"。
     * 200x50 的图上块约 21px、凸舌仅 3px，几乎看不出形状差异。
     */
    private static final double BLOCK_RATIO = 0.42;

    private static final int MIN_BLOCK_SIZE = 24;

    private static final int MAX_BLOCK_SIZE = 80;

    /**
     * 凸舌至少要占这么多像素，否则形状辨识度不足。
     */
    private static final int MIN_READABLE_TAB = 4;

    /**
     * 边缘留白，避免缺口贴边
     */
    private static final int EDGE_MARGIN = 3;

    /**
     * 假缺口数量
     */
    private static final int DECOY_COUNT = 2;

    /**
     * 假缺口的最小水平间距，避免与真缺口重叠
     */
    private static final int DECOY_MIN_GAP = 12;

    /**
     * 自定义底图。设置后所有实例共用，调用方应在启动阶段完成设置。
     */
    private static volatile BufferedImage customBackground;

    /**
     * 设置自定义底图。
     *
     * <p>底图会被缩放到配置的宽高再使用，因此传入任意尺寸都可以，
     * 但建议使用与 {@code easy-captcha.width/height} 比例接近的图，避免拉伸变形。
     *
     * @param image 底图，传 {@code null} 恢复内置底图
     */
    public static void setCustomBackground(BufferedImage image) {
        customBackground = image;
    }

    /**
     * 从输入流加载自定义底图。
     *
     * @param in 图片输入流，支持 PNG/JPG 等 ImageIO 能解析的格式
     * @throws CaptchaGeneratorException 图片无法解析时抛出
     */
    public static void setCustomBackground(InputStream in) {
        try {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new CaptchaGeneratorException("自定义底图无法解析，请确认是有效的图片文件");
            }
            customBackground = image;
        } catch (IOException e) {
            throw new CaptchaGeneratorException("读取自定义底图失败", e);
        }
    }

    /**
     * 当前是否已设置自定义底图。
     */
    public static boolean hasCustomBackground() {
        return customBackground != null;
    }

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.SLIDER_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        int width = config.width();
        int height = config.height();

        int blockSize = (int) Math.round(Math.min(width, height) * BLOCK_RATIO);
        blockSize = Math.max(MIN_BLOCK_SIZE, Math.min(MAX_BLOCK_SIZE, blockSize));
        blockSize = Math.min(blockSize, Math.min(width, height) - EDGE_MARGIN * 2);
        if (PuzzleShape.tabSize(blockSize) < MIN_READABLE_TAB) {
            LOG.warning("滑块拼图尺寸偏小（图片 " + width + "x" + height
                    + "，块 " + blockSize + "px，凸舌仅 " + PuzzleShape.tabSize(blockSize)
                    + "px），建议把 easy-captcha.width/height 调到至少 300x150 以提升辨识度");
        }
        if (blockSize < 16) {
            throw new IllegalArgumentException(
                    "图片尺寸过小，无法生成滑块拼图：width=" + width + ", height=" + height);
        }

        int minX = blockSize + EDGE_MARGIN;
        int maxX = width - blockSize - EDGE_MARGIN;
        if (maxX <= minX) {
            throw new IllegalArgumentException(
                    "图片宽度不足以容纳滑块拼图：width=" + width + ", blockSize=" + blockSize);
        }
        int targetX = RandomUtils.nextInt(minX, maxX);
        int targetY = RandomUtils.nextInt(EDGE_MARGIN,
                Math.max(EDGE_MARGIN + 1, height - blockSize - EDGE_MARGIN));

        // 干净底图：既用来抠滑块，也用来画缺口
        BufferedImage background = renderBaseImage(width, height);

        PuzzleShape shape = PuzzleShape.random();
        // 在**未被污染**的底图上抠出滑块，保证滑块纹理与缺口处完全一致
        BufferedImage blockImage = extractBlock(background, shape, targetX, targetY, blockSize);

        // 画假缺口（先画，让真缺口压在上面）
        List<Point> decoys = pickDecoyPositions(targetX, targetY, blockSize, width, height);
        for (Point decoy : decoys) {
            paintHole(background, shape, decoy.x, decoy.y, blockSize, false);
        }
        // 画真缺口
        paintHole(background, shape, targetX, targetY, blockSize, true);

        SliderChallenge challenge = new SliderChallenge(
                toDataUri(background),
                toDataUri(blockImage),
                blockSize,
                blockSize,
                targetY,
                width,
                height);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("blockWidth", blockSize);
        payload.put("blockHeight", blockSize);
        payload.put("blockY", targetY);
        payload.put("imageWidth", width);
        payload.put("imageHeight", height);
        payload.put("shape", shape.direction());
        payload.put("decoyCount", decoys.size());
        payload.put("slider", challenge.toMap());
        payload.put("captchaType", CaptchaTypeEnum.SLIDER_CAPTCHA);

        return new CaptchaResult(String.valueOf(targetX), encodePng(background), contentType(), payload);
    }

    /**
     * 生成底图：优先自定义，否则内置渐变。
     */
    private BufferedImage renderBaseImage(int width, int height) {
        BufferedImage source = customBackground;
        if (source != null) {
            return scaleAndCrop(source, width, height);
        }
        return drawDefaultBackground(width, height);
    }

    /**
     * 把自定义底图缩放并居中裁剪到目标尺寸，避免变形。
     */
    private BufferedImage scaleAndCrop(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            applyQualityHints(g);
            double scale = Math.max((double) width / source.getWidth(),
                    (double) height / source.getHeight());
            int scaledW = (int) Math.ceil(source.getWidth() * scale);
            int scaledH = (int) Math.ceil(source.getHeight() * scale);
            int offsetX = (width - scaledW) / 2;
            int offsetY = (height - scaledH) / 2;
            g.drawImage(source, offsetX, offsetY, scaledW, scaledH, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /**
     * 内置底图：多段渐变 + 随机色块 + 细线 + 噪点。
     *
     * <p>刻意做得有足够纹理，这样抠出来的滑块才"有东西可对"。
     */
    private BufferedImage drawDefaultBackground(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);

            Color from = new Color(
                    RandomUtils.nextInt(40, 200),
                    RandomUtils.nextInt(40, 200),
                    RandomUtils.nextInt(40, 200));
            Color to = new Color(
                    RandomUtils.nextInt(40, 220),
                    RandomUtils.nextInt(40, 220),
                    RandomUtils.nextInt(40, 220));
            g.setPaint(new GradientPaint(0, 0, from, width, height, to));
            g.fillRect(0, 0, width, height);

            for (int i = 0; i < 18; i++) {
                g.setColor(new Color(
                        RandomUtils.nextInt(30, 230),
                        RandomUtils.nextInt(30, 230),
                        RandomUtils.nextInt(30, 230),
                        RandomUtils.nextInt(70, 170)));
                int w = RandomUtils.nextInt(width / 6, Math.max(width / 6 + 1, width / 2));
                int h = RandomUtils.nextInt(height / 6, Math.max(height / 6 + 1, height / 2));
                int x = RandomUtils.nextInt(-w / 4, Math.max(1, width - w / 2));
                int y = RandomUtils.nextInt(-h / 4, Math.max(1, height - h / 2));
                if (RandomUtils.chance(0.5)) {
                    g.fillOval(x, y, w, h);
                } else {
                    g.fillRect(x, y, w, h);
                }
            }

            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 12; i++) {
                g.setColor(new Color(255, 255, 255, RandomUtils.nextInt(30, 90)));
                g.drawLine(0, RandomUtils.nextInt(0, height), width, RandomUtils.nextInt(0, height));
            }

            for (int i = 0; i < width * height / 24; i++) {
                int x = RandomUtils.nextInt(width);
                int y = RandomUtils.nextInt(height);
                image.setRGB(x, y, RandomUtils.nextInt(0xFFFFFF));
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 按拼图轮廓把目标区域裁成独立小图（带透明背景，形状与缺口一致）。
     */
    private BufferedImage extractBlock(BufferedImage source, PuzzleShape shape,
                                       int targetX, int targetY, int blockSize) {
        BufferedImage block = new BufferedImage(blockSize, blockSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = block.createGraphics();
        try {
            applyQualityHints(g);
            Shape outline = shape.asShape(blockSize);

            // 关键：用形状做裁剪，凸舌部分才能一起被裁出来
            g.setClip(outline);
            g.drawImage(source, -targetX, -targetY, null);
            g.setClip(null);

            // 描边，让滑块在任意背景上都有轮廓
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(255, 255, 255, 200));
            g.draw(outline);
        } finally {
            g.dispose();
        }
        return block;
    }

    /**
     * 在底图上绘制一个缺口。
     *
     * @param real 是���为真缺口。真缺口描边更亮、压暗更明显；假缺口更淡
     */
    private void paintHole(BufferedImage background, PuzzleShape shape,
                           int targetX, int targetY, int blockSize, boolean real) {
        Graphics2D g = background.createGraphics();
        try {
            applyQualityHints(g);
            Shape hole = shape.asShape(blockSize);

            g.translate(targetX, targetY);

            g.setClip(hole);
            g.setColor(new Color(0, 0, 0, real ? 140 : 95));
            g.fill(hole);
            g.setClip(null);

            g.setStroke(new BasicStroke(real ? 1.8f : 1.2f));
            g.setColor(new Color(255, 255, 255, real ? 225 : 140));
            g.draw(hole);
        } finally {
            g.dispose();
        }
    }

    /**
     * 挑选假缺口位置：与真缺口保持足够间距，彼此也不重叠。
     */
    private List<Point> pickDecoyPositions(int realX, int realY, int blockSize,
                                           int width, int height) {
        List<Point> result = new ArrayList<>(DECOY_COUNT);
        int minX = EDGE_MARGIN;
        int maxX = width - blockSize - EDGE_MARGIN;
        int minY = EDGE_MARGIN;
        int maxY = height - blockSize - EDGE_MARGIN;
        if (maxX <= minX || maxY <= minY || width < blockSize * 3) {
            // 画布太窄，放不下假缺口
            return result;
        }

        for (int attempt = 0; attempt < 60 && result.size() < DECOY_COUNT; attempt++) {
            int x = RandomUtils.nextInt(minX, maxX);
            int y = RandomUtils.nextInt(minY, maxY);
            if (Math.abs(x - realX) < blockSize + DECOY_MIN_GAP) {
                continue;
            }
            boolean overlaps = false;
            for (Point existing : result) {
                if (Math.abs(existing.x - x) < blockSize + DECOY_MIN_GAP) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                result.add(new Point(x, y));
            }
        }
        return result;
    }

    /**
     * 图片转 data URI。
     */
    private String toDataUri(BufferedImage image) {
        return contentType().getBase64Prefix()
                + Base64.getEncoder().encodeToString(encodePng(image));
    }
}
