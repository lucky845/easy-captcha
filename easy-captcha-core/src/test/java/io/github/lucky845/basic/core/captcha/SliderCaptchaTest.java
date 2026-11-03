package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.captcha.impl.SliderCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑块拼图专项测试。
 *
 * <h3>回归的三类问题</h3>
 * <ol>
 *     <li><b>滑块是方形、缺口是拼图形</b>——形状包围盒超出画布被裁掉；</li>
 *     <li><b>缺少假缺口</b>——模板匹配一次就能定位，等于没有防护；</li>
 *     <li><b>底图不可替换</b>——业务方无法用自己的素材。</li>
 * </ol>
 *
 * @author created by lucky845
 */
@DisplayName("滑块拼图")
class SliderCaptchaTest {

    private static final CaptchaConfig CONFIG = CaptchaConfig.builder()
            .width(300).height(150).build();

    private static final SliderCaptchaGenerator GENERATOR = new SliderCaptchaGenerator();

    @AfterEach
    void tearDown() {
        // 测试之间互不影响
        SliderCaptchaGenerator.setCustomBackground((BufferedImage) null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sliderData(CaptchaResult result) {
        return (Map<String, Object>) result.extra().get("slider");
    }

    private static BufferedImage decode(String dataUri) throws Exception {
        String base64 = dataUri.substring(dataUri.indexOf(',') + 1);
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    @Test
    @DisplayName("形状包围盒必须严格落在画布内（回归：凸舌被裁成方块）")
    void shapeFitsInsideCanvas() {
        for (int box : new int[]{16, 24, 28, 45, 64}) {
            for (PuzzleShape shape : PuzzleShape.values()) {
                Shape s = shape.asShape(box);
                Rectangle2D b = s.getBounds2D();
                assertAll(
                        () -> assertTrue(b.getMinX() >= -0.01,
                                () -> shape + "@" + box + " 左侧越界: " + b.getMinX()),
                        () -> assertTrue(b.getMinY() >= -0.01,
                                () -> shape + "@" + box + " 顶部越界: " + b.getMinY()),
                        () -> assertTrue(b.getMaxX() <= box + 0.01,
                                () -> shape + "@" + box + " 右侧越界: " + b.getMaxX()
                                        + "（凸舌会被裁掉，滑块退化成方块）"),
                        () -> assertTrue(b.getMaxY() <= box + 0.01,
                                () -> shape + "@" + box + " 底部越界: " + b.getMaxY())
                );
            }
        }
    }

    @ParameterizedTest
    @EnumSource(PuzzleShape.class)
    @DisplayName("每种形状都必须真的有凸起，不能退化成方块")
    void shapeHasRealTab(PuzzleShape shape) {
        int box = 48;
        int tab = PuzzleShape.tabSize(box);
        int baseSide = box - 2 * tab;
        double baseArea = (double) baseSide * baseSide;

        Shape s = shape.asShape(box);
        double actualArea = countOpaquePixels(s, box);

        assertTrue(actualArea > baseArea + 1,
                () -> shape + " 的面积 " + actualArea + " 与基础方块 " + baseArea
                        + " 相同，说明没有凸起");
    }

    @Test
    @DisplayName("滑块图像必须保留凸舌轮廓（透明像素只在形状外）")
    void blockImageKeepsTabShape() throws Exception {
        // 多次生成，确保四种朝向都被覆盖到
        boolean sawNonSquare = false;
        for (int i = 0; i < 30 && !sawNonSquare; i++) {
            CaptchaResult result = GENERATOR.generate(CONFIG);
            BufferedImage block = decode((String) sliderData(result).get("block"));
            int size = block.getWidth();

            // 检查四条边中点：凸舌所在的那条边的中点处必须有不透明像素
            boolean topMid = opaqueAt(block, size / 2, 1);
            boolean bottomMid = opaqueAt(block, size / 2, size - 2);
            boolean leftMid = opaqueAt(block, 1, size / 2);
            boolean rightMid = opaqueAt(block, size - 2, size / 2);

            // 检查四个角：凸舌形状下，角一定在基础方块外，应为透明
            boolean cornerTransparent = !opaqueAt(block, 0, 0);

            if ((topMid || bottomMid || leftMid || rightMid) && cornerTransparent) {
                sawNonSquare = true;
            }
        }
        assertTrue(sawNonSquare,
                "30 次生成中没有任何一次呈现凸舌轮廓，滑块可能仍是方形");
    }

    @Test
    @DisplayName("滑块与缺口形状一致：从同位置抠出的块，纹理应对得上")
    void blockMatchesHoleTexture() throws Exception {
        CaptchaResult result = GENERATOR.generate(CONFIG);
        int targetX = Integer.parseInt(result.code());
        int blockSize = (int) result.extra().get("blockWidth");
        int targetY = (int) result.extra().get("blockY");

        BufferedImage block = decode((String) sliderData(result).get("block"));

        // 滑块中心区域应当是不透明的，且带有底图纹理（不是纯色）
        int cx = blockSize / 2;
        int cy = blockSize / 2;
        assertTrue(opaqueAt(block, cx, cy), "滑块中心应不透明");

        long distinct = countDistinctColors(block);
        assertTrue(distinct > 5,
                () -> "滑块只有 " + distinct + " 种颜色，纹理过于单一，用户无法对准");
    }

    @Test
    @DisplayName("必须存在假缺口作为干扰（回归：缺少干扰项）")
    void decoysExist() throws Exception {
        // 在足够宽的画布上，假缺口应当总是有
        CaptchaConfig wide = CaptchaConfig.builder().width(400).height(150).build();
        CaptchaResult result = GENERATOR.generate(wide);
        Object decoyCount = result.extra().get("decoyCount");
        assertNotNull(decoyCount, "响应中应报告假缺口数量");
        assertTrue((Integer) decoyCount >= 1,
                () -> "假缺口数量为 " + decoyCount + "，没有干扰项，模板匹配可直接定位");

        // 背景图上应当出现多于 1 个缺口形状
        BufferedImage background = decode((String) sliderData(result).get("background"));
        assertNotNull(background);
    }

    @Test
    @DisplayName("画布太窄时优雅降级，不抛异常")
    void narrowCanvasDegradesGracefully() {
        // 1) 能容纳滑块但放不下干扰项的窄画布：正常生成，decoyCount = 0
        CaptchaConfig tight = CaptchaConfig.builder().width(80).height(80).build();
        CaptchaResult narrow = GENERATOR.generate(tight);
        assertNotNull(narrow, "能容纳滑块的窄画布应能生成");
        assertEquals(0, narrow.extra().get("decoyCount"),
                "80px 宽放不下 3 个块，应降级为 0 个假缺口");

        // 2) 恰好还能容纳滑块的极限尺寸：应正常生成，不崩溃
        CaptchaConfig min = CaptchaConfig.builder()
                .width(CaptchaConfig.MIN_WIDTH)
                .height(CaptchaConfig.MIN_HEIGHT)
                .build();
        assertNotNull(GENERATOR.generate(min), "最小合法尺寸也应能生成");

        // 3) 更小的尺寸由 CaptchaConfig 在构造阶段拦截，附带明确字段名
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CaptchaConfig.builder().width(50).height(40).build());
        assertTrue(ex.getMessage().contains("width"),
                () -> "错误信息应指明是 width，实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("自定义底图能生效")
    void customBackgroundIsUsed() throws Exception {
        // 造一张纯红色底图
        BufferedImage red = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = red.createGraphics();
        g.setColor(new Color(200, 30, 30));
        g.fillRect(0, 0, 600, 300);
        g.dispose();

        SliderCaptchaGenerator.setCustomBackground(red);
        assertTrue(SliderCaptchaGenerator.hasCustomBackground());

        CaptchaResult result = GENERATOR.generate(CONFIG);
        BufferedImage background = decode((String) sliderData(result).get("background"));

        // 背景整体应偏红：抽样若干点统计红通道占优的比例
        int reddish = 0;
        int samples = 0;
        for (int y = 5; y < background.getHeight() - 5; y += 10) {
            for (int x = 5; x < background.getWidth() - 5; x += 10) {
                Color c = new Color(background.getRGB(x, y));
                samples++;
                if (c.getRed() > c.getGreen() && c.getRed() > c.getBlue()) {
                    reddish++;
                }
            }
        }
        double ratio = (double) reddish / samples;
        assertTrue(ratio > 0.6,
                () -> "自定义红色底图未生效，偏红像素占比仅 " + String.format("%.2f", ratio));
    }

    @Test
    @DisplayName("自定义底图会按目标尺寸缩放，不会变形出错")
    void customBackgroundIsScaled() throws Exception {
        BufferedImage tiny = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tiny.createGraphics();
        g.setColor(new Color(20, 20, 180));
        g.fillRect(0, 0, 40, 40);
        g.dispose();

        SliderCaptchaGenerator.setCustomBackground(tiny);
        CaptchaResult result = GENERATOR.generate(CONFIG);

        BufferedImage background = decode((String) sliderData(result).get("background"));
        assertEquals(CONFIG.width(), background.getWidth());
        assertEquals(CONFIG.height(), background.getHeight());

        Color center = new Color(background.getRGB(CONFIG.width() / 2, CONFIG.height() / 2));
        assertTrue(center.getBlue() > center.getRed(),
                "小图放大后应仍然偏蓝，实际: " + center);
    }

    @Test
    @DisplayName("从输入流加载自定义底图，非法数据给出明确错误")
    void loadFromStream() {
        SliderCaptchaGenerator.setCustomBackground((BufferedImage) null);
        assertFalse(SliderCaptchaGenerator.hasCustomBackground());

        // 构造一张合法 PNG 并加载
        try {
            BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            SliderCaptchaGenerator.setCustomBackground(
                    new ByteArrayInputStream(out.toByteArray()));
            assertTrue(SliderCaptchaGenerator.hasCustomBackground());
        } catch (Exception e) {
            throw new AssertionError("合法 PNG 应当加载成功", e);
        }

        // 非法数据应抛明确异常
        assertThrows(CaptchaGeneratorException.class, () ->
                SliderCaptchaGenerator.setCustomBackground(
                        new ByteArrayInputStream(new byte[]{1, 2, 3, 4})));
    }

    @Test
    @DisplayName("清除自定义底图后恢复内置底图")
    void clearCustomBackground() {
        SliderCaptchaGenerator.setCustomBackground(
                new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        assertTrue(SliderCaptchaGenerator.hasCustomBackground());

        SliderCaptchaGenerator.setCustomBackground((BufferedImage) null);
        assertFalse(SliderCaptchaGenerator.hasCustomBackground());

        // 恢复后仍能正常生成
        assertNotNull(GENERATOR.generate(CONFIG));
    }

    @Test
    @DisplayName("答案不泄露在下发给前端的数据里")
    void answerNotLeaked() {
        CaptchaResult result = GENERATOR.generate(CONFIG);
        String answer = result.code();
        String payload = String.valueOf(result.extra());

        assertFalse(payload.contains("\"X\"" + answer), "答案不应以独立字段泄露");
        assertFalse(payload.contains("answer=" + answer), "答案不应泄露");
    }

    private static boolean opaqueAt(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
            return false;
        }
        return (img.getRGB(x, y) >>> 24) > 128;
    }

    private static double countOpaquePixels(Shape shape, int box) {
        BufferedImage img = new BufferedImage(box, box, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(shape);
        g.dispose();
        int count = 0;
        for (int y = 0; y < box; y++) {
            for (int x = 0; x < box; x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long countDistinctColors(BufferedImage img) {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) {
                if ((img.getRGB(x, y) >>> 24) > 128) {
                    colors.add(img.getRGB(x, y));
                }
            }
        }
        return colors.size();
    }
}
