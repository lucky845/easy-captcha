package io.github.lucky845.basic.captcha.store;

import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.captcha.PuzzleShape;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 存储的序列化与读写测试。
 *
 * <p>序列化部分直接测 {@code encode}/{@code decode} 的往返，不需要真的连 Redis；
 * 读写语义用 Mockito 模拟 {@link StringRedisTemplate} 验证。
 *
 * @author created by lucky845
 */
@DisplayName("Redis 验证码存储")
class RedisCaptchaStoreTest {

    private static CaptchaResult plain(String code) {
        return new CaptchaResult(code, new byte[]{1, 2, 3}, ContentTypeEnum.PNG);
    }

    @Test
    @DisplayName("往返序列化后答案与图片完全一致")
    void roundTripKeepsCodeAndImage() {
        CaptchaResult original = new CaptchaResult("8F2K", new byte[]{9, 8, 7, 6},
                ContentTypeEnum.GIF, Map.of("captchaType", CaptchaTypeEnum.GIF_CAPTCHA));

        CaptchaStore.Issued decoded = RedisCaptchaStore.decode(
                RedisCaptchaStore.encode(original, 1_700_000_000_000L));

        assertEquals("8F2K", decoded.result().code());
        assertEquals(ContentTypeEnum.GIF, decoded.result().contentType());
        assertArrayEqualsBytes(new byte[]{9, 8, 7, 6}, decoded.result().image());
        assertEquals(1_700_000_000_000L, decoded.issuedAt());
    }

    @Test
    @DisplayName("签发时刻被保留，用于服务端耗时判定")
    void issuedAtIsPreserved() {
        long issuedAt = 1_712_345_678_901L;
        CaptchaStore.Issued decoded = RedisCaptchaStore.decode(
                RedisCaptchaStore.encode(plain("1234"), issuedAt));
        assertEquals(issuedAt, decoded.issuedAt(), "签发时刻必须原样带回，否则无法判断用户耗时");
    }

    @Test
    @DisplayName("extra 里的枚举按 key 还原成枚举，数字保持数字类型")
    void extraKeepsTypes() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("captchaType", CaptchaTypeEnum.SLIDER_CAPTCHA);
        extra.put("shape", PuzzleShape.TAB_RIGHT);
        extra.put("decoyCount", 2);
        extra.put("prompt", "请依次点击：山 水 风");
        extra.put("slider", Boolean.TRUE);

        CaptchaResult original = new CaptchaResult("165", new byte[]{1},
                ContentTypeEnum.PNG, extra);
        CaptchaResult back = RedisCaptchaStore.decode(
                RedisCaptchaStore.encode(original, 1L)).result();

        assertEquals(CaptchaTypeEnum.SLIDER_CAPTCHA, back.extra().get("captchaType"));
        assertEquals(PuzzleShape.TAB_RIGHT, back.extra().get("shape"));
        assertEquals(2, back.extra().get("decoyCount"), "数字必须还原成 Integer，否则前端拿到字符串");
        assertEquals("请依次点击：山 水 风", back.extra().get("prompt"));
        assertEquals(Boolean.TRUE, back.extra().get("slider"));
    }

    @Test
    @DisplayName("内容里带分隔符也不会串字段")
    void separatorsInContentAreSafe() {
        String tricky = "a;b:c 1:2;3";
        CaptchaResult original = new CaptchaResult(tricky, new byte[]{1},
                ContentTypeEnum.PNG, Map.of("prompt", tricky));

        CaptchaResult back = RedisCaptchaStore.decode(
                RedisCaptchaStore.encode(original, 1L)).result();

        assertEquals(tricky, back.code());
        assertEquals(tricky, back.extra().get("prompt"));
    }

    @Test
    @DisplayName("未知枚举值退化成字符串，不影响校验流程")
    void unknownEnumFallsBackToString() {
        CaptchaResult original = new CaptchaResult("1", new byte[]{1},
                ContentTypeEnum.PNG, Map.of("captchaType", "SOME_REMOVED_TYPE"));

        CaptchaResult back = RedisCaptchaStore.decode(
                RedisCaptchaStore.encode(original, 1L)).result();

        assertEquals("SOME_REMOVED_TYPE", back.extra().get("captchaType"));
    }

    @Test
    @DisplayName("保存时写入 TTL，让 Redis 自己回收过期 key")
    void saveSetsTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        new RedisCaptchaStore(redis, Duration.ofMinutes(2), "test:")
                .save("abc", plain("1234"));

        verify(ops).set(eq("test:abc"), anyString(), eq(Duration.ofMinutes(2)));
    }

    @Test
    @DisplayName("取出即删除：第二次取返回空")
    void takeIsOneShot() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        String stored = RedisCaptchaStore.encode(plain("5678"), 100L);
        when(ops.getAndDelete("p:key")).thenReturn(stored, (String) null);

        RedisCaptchaStore store = new RedisCaptchaStore(redis, Duration.ofMinutes(1), "p:");

        Optional<CaptchaStore.Issued> first = store.take("key");
        assertTrue(first.isPresent());
        assertEquals("5678", first.get().result().code());
        assertEquals(100L, first.get().issuedAt());

        assertTrue(store.take("key").isEmpty(), "第二次取出必须为空，保证一次性");
    }

    @Test
    @DisplayName("null 或空白 id 不查 Redis，直接返回空")
    void blankIdReturnsEmpty() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisCaptchaStore store = new RedisCaptchaStore(redis, Duration.ofMinutes(1));

        assertTrue(store.take(null).isEmpty());
        assertTrue(store.take("   ").isEmpty());
        assertTrue(store.remove("").isEmpty());
    }

    @Test
    @DisplayName("非法 TTL 在构造时就报错，而不是等到第一次写入")
    void rejectsNonPositiveTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        assertThrows(IllegalArgumentException.class,
                () -> new RedisCaptchaStore(redis, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new RedisCaptchaStore(redis, Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("损坏的内容不会静默返回空，而是明确报错")
    void corruptedPayloadThrows() {
        assertThrows(IllegalStateException.class,
                () -> RedisCaptchaStore.decode("这不是我们的格式"));
        assertThrows(IllegalStateException.class,
                () -> RedisCaptchaStore.decode("v9;2:ab;"));
    }

    @Test
    @DisplayName("默认前缀是 easy-captcha:")
    void defaultPrefix() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        assertNotNull(new RedisCaptchaStore(redis, Duration.ofMinutes(1)).getPrefix());
        assertEquals("easy-captcha:",
                new RedisCaptchaStore(redis, Duration.ofMinutes(1)).getPrefix());
    }

    private static void assertArrayEqualsBytes(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "第 " + i + " 个字节不一致");
        }
    }
}
