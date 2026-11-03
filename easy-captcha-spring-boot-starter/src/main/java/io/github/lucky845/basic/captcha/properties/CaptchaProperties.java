package io.github.lucky845.basic.captcha.properties;

import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 验证码配置项，统一使用 {@code easy-captcha} 前缀。
 *
 * <pre>
 * easy-captcha:
 *   enabled: true
 *   captcha-type: png_captcha
 *   width: 200
 *   height: 50
 *   captcha-length: 4
 *   char-type: default
 *   font: actionj
 *   difficulty: easy
 *   ttl: 2m
 *   max-entries: 10000
 * </pre>
 *
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = CaptchaProperties.PREFIX)
public class CaptchaProperties {

    /**
     * 配置前缀
     */
    public static final String PREFIX = "easy-captcha";

    /**
     * 是否启用验证码自动配置
     */
    private boolean enabled = true;

    /**
     * 默认验证码类型
     */
    private CaptchaTypeEnum captchaType = CaptchaTypeEnum.PNG_CAPTCHA;

    /**
     * 图片宽度（px）
     */
    private int width = CaptchaConfig.DEFAULT_WIDTH;

    /**
     * 图片高度（px）
     */
    private int height = CaptchaConfig.DEFAULT_HEIGHT;

    /**
     * 验证码字符个数
     */
    private int captchaLength = CaptchaConfig.DEFAULT_LENGTH;

    /**
     * 字符集类型
     */
    private CharTypeEnum charType = CharTypeEnum.DEFAULT;

    /**
     * 字体
     */
    private FontEnum font = FontEnum.ACTIONJ;

    /**
     * 字号，留空则按高度自动推算
     */
    private Float fontSize;

    /**
     * 算术题难度（仅算术验证码生效）
     */
    private ArithmeticDifficultyEnum difficulty = ArithmeticDifficultyEnum.EASY;

    /**
     * 验证码有效期
     */
    private Duration ttl = Duration.ofMinutes(2);

    /**
     * 内存存储的最大条数，超过后按最早过期淘汰
     */
    private int maxEntries = 10_000;

    /**
     * 自定义滑块底图的 classpath 路径，例如 {@code captcha/bg.jpg}。
     *
     * <p>留空则使用内置渐变底图。图片会被缩放并居中裁剪到
     * {@code slider-width} × {@code slider-height}，建议使用 2:1 左右的比例。
     */
    private String sliderBackground;

    /**
     * 滑块拼图的专用宽度。{@code <= 0} 表示沿用全局 {@code width}。
     *
     * <p>滑块拼图对尺寸的要求比其他类型高得多：块太小、凸舌就看不清，
     * 用户无法判断对准没有。200x50 的图上凸舌只有 3px，几乎看不出形状。
     * 建议滑块单独用 300x150 或更大。
     */
    private int sliderWidth = 300;

    /**
     * 滑块拼图的专用高度。{@code <= 0} 表示沿用全局 {@code height}。
     */
    private int sliderHeight = 150;

    /**
     * 滑块偏移量允许的像素误差。
     *
     * <p>拼图块约 28px，8px 约等于块宽的三分之一，属于"明显对准了"的范围。
     */
    private int sliderTolerance = 8;

    /**
     * 点选每次点击允许的像素误差。
     *
     * <p>汉字约 25px 宽，18px 保证点到字的边缘也算命中。
     */
    private int clickTolerance = 18;

    /**
     * 行为验证的轨迹判定阈值。默认宽容档，细节见 {@link BehaviorProperties}。
     */
    private BehaviorProperties behavior = new BehaviorProperties();


    /**
     * 启用 Redis 存储时使用的 key 前缀。
     *
     * <p>只有检测到 {@code StringRedisTemplate} 才会启用 Redis 存储；
     * 前缀用于和同一 Redis 里的其它业务隔离，例如 {@code easy-captcha:}。
     */
    private String redisKeyPrefix = "easy-captcha:";

    /**
     * 限流窗口内允许的签发次数，<= 0 表示不限流
     */
    private int rateLimitPermits = 60;

    /**
     * 限流窗口长度
     */
    private Duration rateLimitWindow = Duration.ofMinutes(1);

    /**
     * 转换为无状态生成器需要的配置对象，并做合法性校验。
     *
     * @return 生成配置
     * @throws IllegalArgumentException 配置项非法时抛出，附带具体字段名
     */
    public CaptchaConfig toCaptchaConfig() {
        int safeWidth = requireRange("width", width, CaptchaConfig.MIN_WIDTH, CaptchaConfig.MAX_WIDTH);
        int safeHeight = requireRange("height", height, CaptchaConfig.MIN_HEIGHT, CaptchaConfig.MAX_HEIGHT);
        int safeLength = requireRange("captcha-length", captchaLength,
                CaptchaConfig.MIN_LENGTH, CaptchaConfig.MAX_LENGTH);
        float safeFontSize = fontSize == null
                ? Math.max(12f, safeHeight * 0.65f)
                : fontSize;
        if (safeFontSize < 8f || safeFontSize > 400f) {
            throw new IllegalArgumentException(
                    "easy-captcha.font-size 必须在 [8, 400] 之间，当前值：" + safeFontSize);
        }
        return new CaptchaConfig(
                safeWidth,
                safeHeight,
                safeLength,
                charType == null ? CharTypeEnum.DEFAULT : charType,
                font == null ? FontEnum.ACTIONJ : font,
                safeFontSize,
                difficulty == null ? ArithmeticDifficultyEnum.EASY : difficulty);
    }

    /**
     * 生成配置。{@code forSlider} 为 true 时优先使用滑块专用宽高。
     *
     * @param forSlider 是否用于滑块验证码
     * @return 生成配置
     */
    public CaptchaConfig toCaptchaConfig(boolean forSlider) {
        if (!forSlider || sliderWidth <= 0 || sliderHeight <= 0) {
            return toCaptchaConfig();
        }
        int safeWidth = requireRange("slider-width", sliderWidth,
                CaptchaConfig.MIN_WIDTH, CaptchaConfig.MAX_WIDTH);
        int safeHeight = requireRange("slider-height", sliderHeight,
                CaptchaConfig.MIN_HEIGHT, CaptchaConfig.MAX_HEIGHT);
        float safeFontSize = fontSize == null ? Math.max(12f, safeHeight * 0.65f) : fontSize;
        return new CaptchaConfig(
                safeWidth,
                safeHeight,
                captchaLength,
                charType == null ? CharTypeEnum.DEFAULT : charType,
                font == null ? FontEnum.ACTIONJ : font,
                safeFontSize,
                difficulty == null ? ArithmeticDifficultyEnum.EASY : difficulty);
    }

    private static int requireRange(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(String.format(
                    "easy-captcha.%s 必须在 [%d, %d] 之间，当前值：%d", name, min, max, value));
        }
        return value;
    }
}
