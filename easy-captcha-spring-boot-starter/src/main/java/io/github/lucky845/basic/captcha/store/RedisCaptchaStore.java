package io.github.lucky845.basic.captcha.store;

import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.captcha.PuzzleShape;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于 Redis 的验证码存储，适用于多实例部署。
 *
 * <p>内存存储只在本进程内有效。一旦应用被部署成多个副本，用户在 A 实例拿到验证码、
 * 提交时被负载均衡打到 B 实例，校验必然失败——这是"本地测着好、上线就挂"的典型原因。
 * 本实现把答案与签发时刻放进 Redis，让任意实例都能校验任意实例签发的验证码。
 *
 * <h3>自动装配条件</h3>
 * <p>只有当调用方自己引入了 {@code spring-boot-starter-data-redis}，并且容器中存在
 * {@link StringRedisTemplate} 时才会注册；否则仍然使用 {@link InMemoryCaptchaStore}。
 *
 * <h3>为什么不用 JDK 序列化</h3>
 * <p>JDK 序列化把类结构焊死在字节流里。升级 jar 后旧条目会反序列化失败
 * （{@code InvalidClassException}），而验证码恰恰是"生成后两分钟内就被读走"的短命数据，
 * 这种失败会直接变成用户侧校验报错。这里改用自描述的长度前缀文本格式：
 * 与版本解耦、redis-cli 里可直接看清内容、也不会因为新增字段就整体不可读。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class RedisCaptchaStore implements CaptchaStore {

    /**
     * 默认 key 前缀
     */
    public static final String DEFAULT_PREFIX = "easy-captcha:";

    /**
     * 序列化格式版本
     */
    private static final String VERSION = "v1";

    private static final char SEPARATOR = ';';

    private static final char LENGTH_DELIMITER = ':';

    private final StringRedisTemplate redis;

    private final long ttlMillis;

    private final String prefix;

    private final Clock clock;

    public RedisCaptchaStore(StringRedisTemplate redis, Duration ttl) {
        this(redis, ttl, DEFAULT_PREFIX, Clock.systemUTC());
    }

    public RedisCaptchaStore(StringRedisTemplate redis, Duration ttl, String prefix) {
        this(redis, ttl, prefix, Clock.systemUTC());
    }

    public RedisCaptchaStore(StringRedisTemplate redis, Duration ttl, String prefix, Clock clock) {
        this.redis = Objects.requireNonNull(redis, "StringRedisTemplate 不能为空");
        Objects.requireNonNull(ttl, "ttl 不能为空");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("easy-captcha.ttl 必须为正数，当前值：" + ttl);
        }
        this.ttlMillis = ttl.toMillis();
        this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
        this.clock = Objects.requireNonNull(clock, "clock 不能为空");
    }

    @Override
    public void save(String id, CaptchaResult result) {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(result, "result 不能为空");
        // TTL 交给 Redis 自己管，不需要写清理任务：
        // 即使应用崩溃、再也没人来读，过期 key 也会被 Redis 回收。
        redis.opsForValue().set(keyOf(id), encode(result, clock.millis()), Duration.ofMillis(ttlMillis));
    }

    @Override
    public Optional<CaptchaResult> remove(String id) {
        return take(id).map(Issued::result);
    }

    /**
     * 原子取出并删除。
     *
     * <p>用 {@code GETDEL}（Spring Data Redis 的 {@code getAndDelete}）而不是
     * "先 GET 再 DEL"：后者在并发提交下会让同一个验证码被读出两次，
     * 一次性语义就破了。
     */
    @Override
    public Optional<Issued> take(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String raw = redis.opsForValue().getAndDelete(keyOf(id));
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(decode(raw));
    }

    /**
     * 当前有效条数。用 SCAN 而不是 KEYS：{@code KEYS} 在大 key 空间下会阻塞整个 Redis。
     */
    @Override
    public int size() {
        int count = 0;
        try (Cursor<String> cursor = scan()) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    @Override
    public void clear() {
        List<String> batch = new ArrayList<>(512);
        try (Cursor<String> cursor = scan()) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 512) {
                    redis.delete(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            redis.delete(batch);
        }
    }

    /**
     * key 前缀，便于在同一 Redis 里和其它业务隔离。
     */
    public String getPrefix() {
        return prefix;
    }

    private Cursor<String> scan() {
        return redis.scan(ScanOptions.scanOptions().match(prefix + "*").count(500).build());
    }

    private String keyOf(String id) {
        return prefix + id;
    }

    // ---------------------------------------------------------------------
    // 序列化：v1;code;contentType;base64Image;issuedAt;extraCount;(key;type;value)*
    // 每个字段 = "<长度>:<内容>;"，长度前缀让内容可以包含任意字符而无需转义。
    // ---------------------------------------------------------------------

    static String encode(CaptchaResult result, long issuedAt) {
        StringBuilder sb = new StringBuilder(256);
        writeField(sb, VERSION);
        writeField(sb, result.code());
        writeField(sb, result.contentType() == null
                ? ContentTypeEnum.PNG.name() : result.contentType().name());
        writeField(sb, result.image() == null || result.image().length == 0
                ? "" : Base64.getEncoder().encodeToString(result.image()));
        writeField(sb, Long.toString(issuedAt));

        Map<String, Object> extra = result.extra();
        writeField(sb, Integer.toString(extra.size()));
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            Object value = entry.getValue();
            writeField(sb, entry.getKey());
            writeField(sb, typeTagOf(value));
            writeField(sb, rawOf(value));
        }
        return sb.toString();
    }

    static Issued decode(String raw) {
        CursorReader reader = new CursorReader(raw);
        String version = reader.read();
        if (!VERSION.equals(version)) {
            throw new IllegalStateException("验证码存储格式版本不支持：" + version);
        }
        String code = reader.read();
        String contentTypeName = reader.read();
        String imageBase64 = reader.read();
        long issuedAt = Long.parseLong(reader.read());
        int extraCount = Integer.parseInt(reader.read());

        Map<String, Object> extra = new LinkedHashMap<>(Math.max(4, extraCount));
        for (int i = 0; i < extraCount; i++) {
            String key = reader.read();
            String typeTag = reader.read();
            String value = reader.read();
            extra.put(key, decodeValue(key, typeTag, value));
        }

        byte[] image = imageBase64 == null || imageBase64.isEmpty()
                ? new byte[0] : Base64.getDecoder().decode(imageBase64);
        CaptchaResult result = new CaptchaResult(code, image,
                parseContentType(contentTypeName), extra);
        return new Issued(result, issuedAt);
    }

    private static void writeField(StringBuilder sb, String value) {
        String safe = value == null ? "" : value;
        sb.append(safe.length()).append(LENGTH_DELIMITER).append(safe).append(SEPARATOR);
    }

    private static String typeTagOf(Object value) {
        if (value == null) {
            return "n";
        }
        if (value instanceof Integer) {
            return "i";
        }
        if (value instanceof Long) {
            return "l";
        }
        if (value instanceof Boolean) {
            return "b";
        }
        if (value instanceof Double || value instanceof Float) {
            return "d";
        }
        if (value instanceof Enum<?>) {
            return "e";
        }
        return "s";
    }

    private static String rawOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 把文本还原成合适的类型。
     *
     * <p>这里刻意<b>不做</b>通用的 {@code Class.forName} 反射还原：那等于给了外部数据
     * 一个加载任意类的能力。枚举只按已知的固定几个 key 白名单还原，其余一律按字符串处理。
     */
    private static Object decodeValue(String key, String typeTag, String raw) {
        if (typeTag == null) {
            return raw;
        }
        return switch (typeTag) {
            case "n" -> null;
            case "i" -> parseIntOr(raw, null);
            case "l" -> parseLongOr(raw, null);
            case "b" -> Boolean.parseBoolean(raw);
            case "d" -> parseDoubleOr(raw, null);
            case "e" -> restoreEnum(key, raw);
            default -> raw;
        };
    }

    private static Object restoreEnum(String key, String raw) {
        if (key == null || raw == null || raw.isEmpty()) {
            return raw;
        }
        try {
            return switch (key) {
                case "captchaType" -> CaptchaTypeEnum.valueOf(raw);
                case "shape" -> PuzzleShape.valueOf(raw);
                default -> raw;
            };
        } catch (IllegalArgumentException e) {
            // 旧数据里可能是已删除的枚举值，退化成字符串而不是让整个校验报错
            return raw;
        }
    }

    private static ContentTypeEnum parseContentType(String name) {
        if (name == null || name.isEmpty()) {
            return ContentTypeEnum.PNG;
        }
        try {
            return ContentTypeEnum.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ContentTypeEnum.PNG;
        }
    }

    private static Integer parseIntOr(String raw, Integer fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long parseLongOr(String raw, Long fallback) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Double parseDoubleOr(String raw, Double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * 长度前缀字段的顺序读取器。
     */
    private static final class CursorReader {

        private final String source;

        private int pos;

        CursorReader(String source) {
            this.source = Objects.requireNonNull(source, "待解析内容不能为空");
        }

        String read() {
            int colon = source.indexOf(LENGTH_DELIMITER, pos);
            if (colon < 0) {
                throw new IllegalStateException("验证码存储内容损坏：缺少长度标记，位置 " + pos);
            }
            int length;
            try {
                length = Integer.parseInt(source.substring(pos, colon));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("验证码存储内容损坏：长度不是数字，位置 " + pos, e);
            }
            if (length < 0) {
                throw new IllegalStateException("验证码存储内容损坏：长度为负，位置 " + pos);
            }
            int start = colon + 1;
            int end = start + length;
            if (end > source.length()) {
                throw new IllegalStateException("验证码存储内容损坏：字段被截断，位置 " + pos);
            }
            pos = end;
            if (pos < source.length() && source.charAt(pos) == SEPARATOR) {
                pos++;
            }
            return source.substring(start, end);
        }
    }
}
