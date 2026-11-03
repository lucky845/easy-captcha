package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.captcha.impl.ArithmeticCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ChineseCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ChineseGifCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.ClickWordCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.GifCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.PngCaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.SliderCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生成器行为测试。
 *
 * <p>重点回归历史 bug：旧实现在同一个实例上反复生成时，验证码文本会永久冻结。
 *
 * @author created by lucky845
 */
@DisplayName("验证码生成器")
class CaptchaGeneratorTest {

    private static final CaptchaConfig CONFIG = CaptchaConfig.defaults();

    private static List<CaptchaGenerator> allGenerators() {
        return List.of(
                new PngCaptchaGenerator(),
                new GifCaptchaGenerator(),
                new ChineseCaptchaGenerator(),
                new ChineseGifCaptchaGenerator(),
                new ArithmeticCaptchaGenerator(),
                new SliderCaptchaGenerator(),
                new ClickWordCaptchaGenerator()
        );
    }

    @Test
    @DisplayName("同一个生成器实例连续生成，答案必须每次都不一样（回归：文本冻结）")
    void answerMustChangeAcrossCalls() {
        for (CaptchaGenerator generator : allGenerators()) {
            Set<String> codes = new HashSet<>();
            int rounds = 200;
            for (int i = 0; i < rounds; i++) {
                codes.add(generator.generate(CONFIG).code());
            }
            // 绝对不能只有 1 种答案——那就是旧版"验证码永久冻结"的病症。
            assertTrue(codes.size() > 1,
                    () -> generator.captchaType() + " 连续 200 次只产出 " + codes.size()
                            + " 种答案，验证码被冻结");

            // 不同验证码的"答案空间"天然不同，阈值必须按类型区分，
            // 否则会写出看起来严格、实际随机失败的 flaky 断言。
            int expectedDistinct = switch (generator.captchaType()) {
                // 一位数加减，答案只落在 0~18
                case ARITHMETIC_CAPTCHA -> 12;
                // 答案 6 位字符，空间 54^6
                case PNG_CAPTCHA, GIF_CAPTCHA -> rounds / 2;
                // 中文常用字约 500，取 2~4 个字
                case CHINESE_CAPTCHA, CHINESE_GIF_CAPTCHA -> rounds / 2;
                // 答案是缺口 X 坐标，200px 宽、块宽 28px 时只有约 136 个可能值，
                // 200 次采样的期望唯一值约 105，这里取保守下限
                case SLIDER_CAPTCHA -> 60;
                // 答案是坐标串，空间极大
                case CLICK_WORD_CAPTCHA -> rounds / 2;
            };
            assertTrue(codes.size() >= expectedDistinct,
                    () -> generator.captchaType() + " 200 次只产出 " + codes.size()
                            + " 种答案，低于预期 " + expectedDistinct);
        }
    }

    @Test
    @DisplayName("同一个生成器实例连续生成，图片字节必须每次都不一样")
    void imageMustChangeAcrossCalls() {
        for (CaptchaGenerator generator : allGenerators()) {
            CaptchaResult first = generator.generate(CONFIG);
            CaptchaResult second = generator.generate(CONFIG);
            assertNotEquals(first.code() + "|" + first.size(), second.code() + "|" + second.size(),
                    () -> generator.captchaType() + " 连续两次生成的图片完全一致");
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CaptchaTypeEnum.class)
    @DisplayName("每种验证码类型都能生成合法结果")
    void everyTypeProducesValidResult(CaptchaTypeEnum type) {
        CaptchaGenerator generator = allGenerators().stream()
                .filter(g -> g.captchaType() == type)
                .findFirst()
                .orElseThrow();
        CaptchaResult result = generator.generate(CONFIG);

        assertAll(
                () -> assertNotNull(result.code(), "答案不能为空"),
                () -> assertFalse(result.code().isBlank(), "答案不能为空白"),
                () -> assertTrue(result.size() > 0, "图片字节不能为 0"),
                () -> assertNotNull(result.contentType()),
                () -> assertTrue(result.toBase64().startsWith("data:image/"),
                        "Base64 必须以 data:image/ 开头")
        );
    }

    @Test
    @DisplayName("生成的图片是可解析的图像")
    void imageMustBeDecodable() throws Exception {
        for (CaptchaGenerator generator : allGenerators()) {
            CaptchaResult result = generator.generate(CONFIG);
            var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(result.image()));
            assertNotNull(image, () -> generator.captchaType() + " 生成的图片无法被 ImageIO 解析");
            assertEquals(CONFIG.width(), image.getWidth(),
                    () -> generator.captchaType() + " 图片宽度不符");
            assertEquals(CONFIG.height(), image.getHeight(),
                    () -> generator.captchaType() + " 图片高度不符");
        }
    }

    @Test
    @DisplayName("内容类型与声明一致")
    void contentTypeMatchesImageFormat() {
        assertEquals(ContentTypeEnum.PNG, new PngCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.GIF, new GifCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.PNG, new ChineseCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.GIF, new ChineseGifCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.PNG, new ArithmeticCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.PNG, new SliderCaptchaGenerator().contentType());
        assertEquals(ContentTypeEnum.PNG, new ClickWordCaptchaGenerator().contentType());
    }

    @Test
    @DisplayName("类型枚举与注册的生成器一一对应，不多不少")
    void everyEnumHasExactlyOneGenerator() {
        Set<CaptchaTypeEnum> fromGenerators = allGenerators().stream()
                .map(CaptchaGenerator::captchaType)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Arrays.stream(CaptchaTypeEnum.values()).collect(
                java.util.stream.Collectors.toSet()), fromGenerators,
                "枚举里有类型没有对应生成器，或生成器未登记类型");
    }

    @Test
    @DisplayName("字符数与配置一致")
    void lengthMatchesConfig() {
        CaptchaConfig config = CaptchaConfig.builder().length(6).build();
        assertEquals(6, new PngCaptchaGenerator().generate(config).code().length());
        assertEquals(6, new GifCaptchaGenerator().generate(config).code().length());
        assertEquals(6, new ChineseCaptchaGenerator().generate(config).code().length());
        assertEquals(6, new ChineseGifCaptchaGenerator().generate(config).code().length());
    }

    @Test
    @DisplayName("纯数字类型只产出数字")
    void pureNumberOnlyDigits() {
        CaptchaConfig config = CaptchaConfig.builder()
                .charType(CharTypeEnum.PURE_NUMBER)
                .length(8)
                .build();
        for (int i = 0; i < 50; i++) {
            String code = new PngCaptchaGenerator().generate(config).code();
            assertTrue(code.chars().allMatch(Character::isDigit), "出现非数字字符：" + code);
        }
    }

    @Test
    @DisplayName("算术验证码的答案与图片上的算式一致")
    void arithmeticAnswerMatchesExpression() {
        ArithmeticCaptchaGenerator generator = new ArithmeticCaptchaGenerator();
        for (ArithmeticDifficultyEnum difficulty : ArithmeticDifficultyEnum.values()) {
            CaptchaConfig config = CaptchaConfig.builder().difficulty(difficulty).build();
            for (int i = 0; i < 30; i++) {
                CaptchaResult result = generator.generate(config);
                int answer = Integer.parseInt(result.code());
                assertTrue(answer >= 0, "答案不能为负数：" + answer);
                assertTrue(answer <= 100, "答案超出上限：" + answer);
            }
        }
    }

    @Test
    @DisplayName("极端尺寸不会抛异常")
    void extremeSizesWork() {
        CaptchaConfig tiny = CaptchaConfig.builder().width(60).height(30).length(1).build();
        CaptchaConfig large = CaptchaConfig.builder().width(800).height(300).length(10).build();
        for (CaptchaGenerator generator : allGenerators()) {
            assertNotNull(generator.generate(tiny));
            assertNotNull(generator.generate(large));
        }
    }

    @Test
    @DisplayName("全部内置字体都能加载")
    void allFontsLoad() {
        for (FontEnum font : FontEnum.values()) {
            CaptchaConfig config = CaptchaConfig.builder().font(font).build();
            CaptchaResult result = new PngCaptchaGenerator().generate(config);
            assertTrue(result.size() > 0, () -> font + " 生成的图片为空");
        }
    }
}
