package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.CaptchaStore;
import io.github.lucky845.basic.captcha.store.RedisCaptchaStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.logging.Logger;

/**
 * Redis 存储自动配置。
 *
 * <p>只有当调用方自己引入了 {@code spring-boot-starter-data-redis}、容器里也确实存在
 * {@link StringRedisTemplate} 时才会生效。这个类是独立的自动配置，并且用
 * {@code before = CaptchaAutoconfiguration.class} 声明必须先于内存存储求值——
 * 否则 {@link CaptchaAutoconfiguration#captchaStore} 上的
 * {@code @ConditionalOnMissingBean} 会先看到内存实现，把 Redis 实现挤掉。
 *
 * <h3>为什么需要它</h3>
 * <p>内存存储只在本进程内有效。应用一旦被部署成多个副本，用户在 A 实例拿到验证码、
 * 提交时被负载均衡打到 B 实例，校验必然失败。这类问题在单机联调时完全看不出来，
 * 只有上线后才会暴露。有了本配置，多副本部署下验证码在集群内共享。
 *
 * @author created by lucky845 on 2025-01-16
 */
@AutoConfiguration(
        before = CaptchaAutoconfiguration.class,
        // 用字符串而不是类字面量：StringRedisTemplate 不存在时这个类根本不该被加载，
        // 直接引用 RedisAutoConfiguration.class 会在类加载阶段就抛 NoClassDefFoundError。
        afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = CaptchaProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaRedisAutoconfiguration {

    private static final Logger LOG = Logger.getLogger(CaptchaRedisAutoconfiguration.class.getName());

    /**
     * 检测到 Redis 时，用 Redis 保存验证码，替换默认的内存实现。
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaStore.class)
    public CaptchaStore captchaStore(StringRedisTemplate redisTemplate, CaptchaProperties properties) {
        String prefix = properties.getRedisKeyPrefix();
        LOG.info("easy-captcha: 检测到 StringRedisTemplate，启用 Redis 存储（key 前缀 "
                + prefix + "，TTL " + properties.getTtl() + "）");
        return new RedisCaptchaStore(redisTemplate, properties.getTtl(), prefix);
    }
}
