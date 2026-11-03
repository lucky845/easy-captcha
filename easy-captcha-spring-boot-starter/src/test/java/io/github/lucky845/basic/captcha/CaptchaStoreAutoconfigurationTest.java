package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.store.CaptchaStore;
import io.github.lucky845.basic.captcha.store.InMemoryCaptchaStore;
import io.github.lucky845.basic.captcha.store.RedisCaptchaStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 存储实现的自动选择逻辑。
 *
 * <p>关键行为：没有 Redis 就用内存，有 Redis 就自动切过去。
 * 这条链路一旦选错，多副本部署下验证码会全部校验失败，而且单机测试完全发现不了。
 *
 * @author created by lucky845
 */
@DisplayName("存储自动选择")
class CaptchaStoreAutoconfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CaptchaRedisAutoconfiguration.class,
                    CaptchaAutoconfiguration.class));

    @Test
    @DisplayName("没有 StringRedisTemplate 时使用内存存储")
    void fallsBackToMemoryWithoutRedis() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CaptchaStore.class);
            assertThat(context.getBean(CaptchaStore.class))
                    .isInstanceOf(InMemoryCaptchaStore.class);
        });
    }

    @Test
    @DisplayName("存在 StringRedisTemplate 时自动切换到 Redis 存储")
    void switchesToRedisWhenTemplatePresent() {
        runner.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(CaptchaStore.class);
                    assertThat(context.getBean(CaptchaStore.class))
                            .isInstanceOf(RedisCaptchaStore.class);
                });
    }

    @Test
    @DisplayName("用户自定义 CaptchaStore 优先级最高，不被内置实现覆盖")
    void customStoreWins() {
        runner.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean("myStore", CaptchaStore.class, () -> new InMemoryCaptchaStore(
                        java.time.Duration.ofMinutes(1), 10))
                .run(context -> {
                    assertThat(context).hasSingleBean(CaptchaStore.class);
                    assertThat(context.getBean(CaptchaStore.class))
                            .isInstanceOf(InMemoryCaptchaStore.class);
                });
    }

    @Test
    @DisplayName("easy-captcha.enabled=false 时整套自动配置都不生效")
    void disabledSkipsEverything() {
        runner.withPropertyValues("easy-captcha.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CaptchaStore.class));
    }

    @Test
    @DisplayName("Redis key 前缀可通过配置覆盖")
    void redisPrefixIsConfigurable() {
        runner.withPropertyValues("easy-captcha.redis-key-prefix=custom:")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    RedisCaptchaStore store = (RedisCaptchaStore) context.getBean(CaptchaStore.class);
                    assertThat(store.getPrefix()).isEqualTo("custom:");
                });
    }
}
