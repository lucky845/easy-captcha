package io.github.lucky845.basic.captcha.store;

import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内存存储测试。
 *
 * @author created by lucky845
 */
@DisplayName("内存验证码存储")
class InMemoryCaptchaStoreTest {

    private static CaptchaResult result(String code) {
        return new CaptchaResult(code, new byte[]{1, 2, 3}, ContentTypeEnum.PNG);
    }

    @Test
    @DisplayName("保存后能取出，取出即删除")
    void saveThenRemoveOnce() {
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMinutes(1), 100);
        store.save("a", result("1234"));

        Optional<CaptchaResult> first = store.remove("a");
        assertTrue(first.isPresent());
        assertEquals("1234", first.get().code());

        assertTrue(store.remove("a").isEmpty(), "第二次取出应为空（一次性）");
    }

    @Test
    @DisplayName("过期条目取不出来")
    void expiredEntryIsGone() throws Exception {
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMillis(50), 100);
        store.save("a", result("1234"));
        Thread.sleep(120);
        assertTrue(store.remove("a").isEmpty(), "过期条目仍可取出");
    }

    @Test
    @DisplayName("超过 maxEntries 会淘汰，不会无限增长")
    void capacityIsBounded() {
        int cap = 10;
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMinutes(1), cap);
        for (int i = 0; i < cap * 5; i++) {
            store.save("k" + i, result("code" + i));
        }
        assertTrue(store.size() <= cap,
                () -> "存储条数 " + store.size() + " 超过上限 " + cap);
    }

    @Test
    @DisplayName("clear 清空全部")
    void clearRemovesEverything() {
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMinutes(1), 100);
        store.save("a", result("1"));
        store.save("b", result("2"));
        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("null id 不会抛异常")
    void nullIdSafe() {
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMinutes(1), 100);
        assertTrue(store.remove(null).isEmpty());
        assertThrows(NullPointerException.class, () -> store.save(null, result("1")));
    }

    @Test
    @DisplayName("非法构造参数立即报错")
    void invalidConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new InMemoryCaptchaStore(Duration.ZERO, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new InMemoryCaptchaStore(Duration.ofMinutes(1), 0));
    }

    @Test
    @DisplayName("不存在的 id 返回空")
    void missingIdReturnsEmpty() {
        InMemoryCaptchaStore store = new InMemoryCaptchaStore(Duration.ofMinutes(1), 100);
        assertFalse(store.remove("nope").isPresent());
    }
}
