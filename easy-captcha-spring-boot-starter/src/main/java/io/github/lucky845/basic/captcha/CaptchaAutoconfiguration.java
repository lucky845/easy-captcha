package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.behavior.CaptchaRateLimiter;
import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorRegistry;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.CaptchaStore;
import io.github.lucky845.basic.captcha.store.InMemoryCaptchaStore;
import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import io.github.lucky845.basic.core.captcha.CaptchaGenerator;
import io.github.lucky845.basic.core.captcha.impl.SliderCaptchaGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 验证码自动配置。
 *
 * <p>所有 Bean 都是同步创建的，不存在"异步注册未完成请求先到"的竞态；
 * 每个 Bean 都带 {@link ConditionalOnMissingBean}，调用方可以按需替换。
 *
 * @author created by lucky845 on 2025-01-16
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoconfiguration {

    private static final Logger LOG = Logger.getLogger(CaptchaAutoconfiguration.class.getName());

    /**
     * 注册表：内置 5 种生成器 + 容器里所有自定义 {@link CaptchaGenerator}（同类型覆盖内置）。
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaGeneratorRegistry captchaGeneratorRegistry(
            ObjectProvider<CaptchaGenerator> generators,
            CaptchaProperties properties) {
        loadSliderBackground(properties);
        return new CaptchaGeneratorRegistry(generators.orderedStream().toList());
    }

    /**
     * 如果配置了自定义滑块底图，就在容器启动阶段加载一次。
     *
     * <p>加载失败只记录警告，不阻断启动——底图只是观感，不该让整个应用起不来。
     */
    private void loadSliderBackground(CaptchaProperties properties) {
        String location = properties.getSliderBackground();
        if (location == null || location.isBlank()) {
            return;
        }
        String normalized = location.startsWith("/") ? location : "/" + location;
        try (var in = getClass().getResourceAsStream(normalized)) {
            if (in == null) {
                LOG.warning("找不到自定义滑块底图：" + normalized + "，将使用内置底图");
                return;
            }
            SliderCaptchaGenerator.setCustomBackground(in);
            LOG.info("已加载自定义滑块底图：" + normalized);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "加载自定义滑块底图失败：" + normalized + "，将使用内置底图", e);
        }
    }

    /**
     * 默认使用内存存储；接入 Redis 时提供自己的 {@link CaptchaStore} 实现即可覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaStore.class)
    public CaptchaStore captchaStore(CaptchaProperties properties) {
        return new InMemoryCaptchaStore(properties.getTtl(), properties.getMaxEntries());
    }

    /**
     * 拖动型（滑块）阈值，由 {@code easy-captcha.behavior.drag.*} 配置。
     */
    @Bean
    @ConditionalOnMissingBean(name = "captchaDragTuning")
    public BehaviorTuning captchaDragTuning(CaptchaProperties properties) {
        return properties.getBehavior().dragTuning();
    }

    /**
     * 点击型（点选）阈值，由 {@code easy-captcha.behavior.click.*} 配置。
     */
    @Bean
    @ConditionalOnMissingBean(name = "captchaClickTuning")
    public BehaviorTuning captchaClickTuning(CaptchaProperties properties) {
        return properties.getBehavior().clickTuning();
    }

    /**
     * 签发限流器。permits <= 0 时等价于不限流。
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaRateLimiter captchaRateLimiter(CaptchaProperties properties) {
        int permits = properties.getRateLimitPermits();
        long window = properties.getRateLimitWindow().toMillis();
        if (permits <= 0) {
            return new CaptchaRateLimiter(Integer.MAX_VALUE, Math.max(1, window));
        }
        return new CaptchaRateLimiter(permits, Math.max(1, window));
    }

    /**
     * 无状态验证码服务。
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(CaptchaGeneratorRegistry registry,
                                         CaptchaProperties properties,
                                         CaptchaStore store,
                                         @Qualifier("captchaDragTuning") BehaviorTuning dragTuning,
                                         @Qualifier("captchaClickTuning") BehaviorTuning clickTuning,
                                         CaptchaRateLimiter rateLimiter) {
        return new CaptchaService(registry, properties, store, dragTuning, clickTuning, rateLimiter);
    }
}
