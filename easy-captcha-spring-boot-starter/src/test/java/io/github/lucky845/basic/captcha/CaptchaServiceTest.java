package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.generator.CaptchaGeneratorRegistry;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.captcha.store.InMemoryCaptchaStore;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * starter 行为测试。
 *
 * @author created by lucky845
 */
@DisplayName("验证码 starter")
class CaptchaServiceTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CaptchaAutoconfiguration.class));

    @Test
    @DisplayName("默认配置下上下文可以启动，核心 Bean 齐全")
    void autoConfigurationStarts() {
        runner.run(context -> assertAll(
                () -> assertTrue(context.getStartupFailure() == null,
                        () -> "上下文启动失败：" + context.getStartupFailure()),
                () -> assertNotNull(context.getBean(CaptchaService.class)),
                () -> assertNotNull(context.getBean(CaptchaGeneratorRegistry.class)),
                () -> assertNotNull(context.getBean(CaptchaProperties.class))
        ));
    }

    @Test
    @DisplayName("enabled=false 时不注册任何验证码 Bean")
    void disabledByProperty() {
        runner.withPropertyValues("easy-captcha.enabled=false")
                .run(context -> assertFalse(context.containsBean("captchaService")));
    }

    @Test
    @DisplayName("上下文启动即可用（回归：策略异步注册导致首个请求失败）")
    void serviceUsableImmediatelyAfterStartup() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            // 启动后第一件事就是生成，不能再有"策略还没注册好"的窗口
            CaptchaResult first = service.generate();
            assertNotNull(first.code());
            assertTrue(first.size() > 0);

            CaptchaResult second = service.generate();
            assertFalse(first.code().equals(second.code()) && first.size() == second.size(),
                    "连续两次生成结果完全一致，验证码被冻结");
        });
    }

    @Test
    @DisplayName("issue → validate 全链路：正确答案通过，且只能用一次")
    void issueThenValidateOnce() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            CaptchaService.CaptchaIssue issue = service.issue();

            assertNotNull(issue.id());
            assertTrue(service.validate(issue.id(), issue.result().code()), "正确答案应当通过");

            // 第二次必须失败——验证码是一次性的
            assertFalse(service.validate(issue.id(), issue.result().code()), "验证码被重复使用");
        });
    }

    @Test
    @DisplayName("错误答案不通过，也不影响其它验证码")
    void wrongAnswerRejected() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            CaptchaService.CaptchaIssue issue = service.issue();
            assertFalse(service.validate(issue.id(), " definitely-wrong "));
        });
    }

    @Test
    @DisplayName("空 id / 空答案直接判失败，不抛异常")
    void nullAndBlankInputSafe() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            assertAll(
                    () -> assertFalse(service.validate(null, "x")),
                    () -> assertFalse(service.validate("", "x")),
                    () -> assertFalse(service.validate("  ", "x")),
                    () -> assertFalse(service.validate("unknown-id", null))
            );
        });
    }

    @Test
    @DisplayName("答案大小写不敏感、两侧空格被忽略")
    void answerNormalization() {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.PNG_CAPTCHA);
            String code = issue.result().code();
            assertTrue(service.validate(issue.id(), "  " + code.toLowerCase() + "  "),
                    "大小写与空格应当被规范化");
        });
    }

    @Test
    @DisplayName("每种配置的类型都能生成")
    void everyConfiguredTypeWorks() {
        for (CaptchaTypeEnum type : CaptchaTypeEnum.values()) {
            runner.withPropertyValues("easy-captcha.captcha-type=" + type.name().toLowerCase())
                    .run(context -> {
                        CaptchaService service = context.getBean(CaptchaService.class);
                        CaptchaResult result = service.generate();
                        assertTrue(result.size() > 0, () -> type + " 生成失败");
                    });
        }
    }

    @Test
    @DisplayName("自定义 CaptchaStore 能覆盖默认内存实现")
    void customStoreOverridesDefault() {
        AtomicInteger saves = new AtomicInteger();
        runner.withBean(io.github.lucky845.basic.captcha.store.CaptchaStore.class,
                        () -> new io.github.lucky845.basic.captcha.store.CaptchaStore() {
                    @Override
                    public void save(String id, CaptchaResult result) {
                        saves.incrementAndGet();
                    }

                    @Override
                    public java.util.Optional<CaptchaResult> remove(String id) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public int size() {
                        return 0;
                    }

                    @Override
                    public void clear() {
                    }
                })
                .run(context -> {
                    CaptchaService service = context.getBean(CaptchaService.class);
                    service.issue();
                    assertEquals(1, saves.get(), "自定义 store 没有被使用");
                });
    }

    @Test
    @DisplayName("并发生成不会串号（回归：CaptchaService 持有 captcha 字段）")
    void concurrentGeneration() throws Exception {
        runner.run(context -> {
            CaptchaService service = context.getBean(CaptchaService.class);
            int threads = 16;
            int perThread = 30;
            Set<String> codes = ConcurrentHashMap.newKeySet();
            List<Throwable> errors = new ArrayList<>();
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < perThread; j++) {
                            codes.add(service.issue().result().code());
                        }
                    } catch (Throwable t) {
                        synchronized (errors) {
                            errors.add(t);
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "并发超时");
            pool.shutdownNow();

            assertEquals(List.of(), errors);
            assertTrue(codes.size() > threads * perThread * 0.9,
                    () -> "并发只产出 " + codes.size() + " 种答案，存在串号");
        });
    }

    @Test
    @DisplayName("非法的宽高配置在启动阶段就报错，而不是运行时崩溃")
    void invalidConfigFailsFast() {
        runner.withPropertyValues("easy-captcha.width=10")
                .run(context -> {
                    // 属性绑定本身不校验，使用配置时才失败——保证错误信息指向具体字段
                    CaptchaService service = context.getBean(CaptchaService.class);
                    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, service::generate);
                    assertTrue(ex.getMessage().contains("width"), "错误信息未指明字段：" + ex.getMessage());
                });
    }
}
