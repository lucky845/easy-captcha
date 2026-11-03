package io.github.lucky845.basic.captcha.store;

import io.github.lucky845.basic.core.result.CaptchaResult;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * 基于 {@link ConcurrentHashMap} 的内存验证码存储。
 *
 * <p>带 TTL 与容量上限：每 64 次写入做一轮过期清理；超过 {@code maxEntries} 时
 * 按最早过期时间淘汰，防止被刷接口打爆内存。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class InMemoryCaptchaStore implements CaptchaStore {

    private static final Logger LOG = Logger.getLogger(InMemoryCaptchaStore.class.getName());

    /**
     * 每写入这么多次做一次全量过期清理
     */
    private static final int CLEANUP_INTERVAL = 64;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private final AtomicLong writeCounter = new AtomicLong();

    private final long ttlMillis;

    private final int maxEntries;

    /**
     * 时钟。生产用系统时钟，测试可注入可控时钟验证 TTL 与耗时判定。
     */
    private final Clock clock;

    public InMemoryCaptchaStore(Duration ttl, int maxEntries) {
        this(ttl, maxEntries, Clock.systemUTC());
    }

    public InMemoryCaptchaStore(Duration ttl, int maxEntries, Clock clock) {
        Objects.requireNonNull(ttl, "ttl 不能为空");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("easy-captcha.ttl 必须为正数，当前值：" + ttl);
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("easy-captcha.max-entries 必须大于 0，当前值：" + maxEntries);
        }
        this.ttlMillis = ttl.toMillis();
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    @Override
    public void save(String id, CaptchaResult result) {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(result, "result 不能为空");
        long now = clock.millis();
        entries.put(id, new Entry(result, now, now + ttlMillis));
        if (writeCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            evictExpired(now);
        }
        if (entries.size() > maxEntries) {
            evictOverflow(now);
        }
    }

    @Override
    public Optional<CaptchaResult> remove(String id) {
        return take(id).map(Issued::result);
    }

    /**
     * 取出并删除，同时带回服务端写入的签发时刻。
     *
     * <p>签发时刻来自服务端时钟（{@link System#currentTimeMillis()}），
     * 与客户端上报的任何时间字段无关，因此不可伪造。
     */
    @Override
    public Optional<Issued> take(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Entry entry = entries.remove(id);
        if (entry == null || entry.expireAt() <= clock.millis()) {
            return Optional.empty();
        }
        return Optional.of(new Issued(entry.result(), entry.issuedAt()));
    }

    @Override
    public int size() {
        evictExpired(clock.millis());
        return entries.size();
    }

    @Override
    public void clear() {
        entries.clear();
        writeCounter.set(0);
    }

    private void evictExpired(long now) {
        entries.entrySet().removeIf(e -> e.getValue().expireAt() <= now);
    }

    private void evictOverflow(long now) {
        evictExpired(now);
        while (entries.size() > maxEntries) {
            String oldestKey = null;
            long oldestExpireAt = Long.MAX_VALUE;
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                long expireAt = entry.getValue().expireAt();
                if (expireAt < oldestExpireAt) {
                    oldestExpireAt = expireAt;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey == null) {
                return;
            }
            entries.remove(oldestKey);
            LOG.fine(() -> "验证码存储达到上限 " + maxEntries + "，淘汰最早的条目");
        }
    }

    /**
     * 存储条目
     *
     * @param result   验证码结果
     * @param issuedAt 服务端签发时刻（毫秒）
     * @param expireAt 过期时间戳（毫秒）
     */
    private record Entry(CaptchaResult result, long issuedAt, long expireAt) {
    }
}
