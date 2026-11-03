package io.github.lucky845.basic.captcha;

import io.github.lucky845.basic.captcha.behavior.BehaviorAnswer;
import io.github.lucky845.basic.captcha.properties.BehaviorProperties;
import io.github.lucky845.basic.captcha.properties.CaptchaProperties;
import io.github.lucky845.basic.core.behavior.BehaviorTuning;
import io.github.lucky845.basic.core.behavior.BehaviorVerdict;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阈值配置化测试。
 *
 * <p>核心验收点：用户改配置，行为判定必须真的跟着变。
 *
 * @author created by lucky845
 */
@DisplayName("行为阈值可配置")
class BehaviorConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CaptchaAutoconfiguration.class));

    private static List<BehaviorAnswer.Point> points(int to, long step) {
        return List.of(
                new BehaviorAnswer.Point(0, 0, 0),
                new BehaviorAnswer.Point(to / 2, 2, step),
                new BehaviorAnswer.Point(to, 1, step * 2));
    }

    @Nested
    @DisplayName("默认值是宽容档")
    class DefaultsAreLenient {

        @Test
        @DisplayName("默认不设耗时下限、不限点数、不查弯曲度")
        void defaults() {
            runner.run(ctx -> {
                BehaviorTuning drag = ctx.getBean("captchaDragTuning", BehaviorTuning.class);
                BehaviorTuning click = ctx.getBean("captchaClickTuning", BehaviorTuning.class);

                assertEquals(0L, drag.minDurationMs(), "拖动默认不应设耗时下限");
                assertEquals(1, drag.minPoints(), "拖动默认不应限制点数");
                assertFalse(drag.checkCurvature(), "拖动默认不应查弯曲度");
                assertEquals(60, drag.monotonicTolerance(), "拖动回退容差应为 60px");
                assertEquals(300_000L, drag.maxDurationMs());

                assertEquals(0L, click.minDurationMs(), "点选默认不应设耗时下限");
                assertEquals(Integer.MAX_VALUE, click.monotonicTolerance(),
                        "点选不应做方向判定");
                assertFalse(click.checkCurvature(), "点选不应查弯曲度");

                CaptchaProperties props = ctx.getBean(CaptchaProperties.class);
                assertEquals(8, props.getSliderTolerance());
                assertEquals(18, props.getClickTolerance());
            });
        }

        @Test
        @DisplayName("默认配置下，极快提交也能通过（真人可用性基线）")
        void fastSubmissionPassesByDefault() {
            runner.run(ctx -> {
                CaptchaService service = ctx.getBean(CaptchaService.class);
                CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                int target = Integer.parseInt(issue.result().code());

                // 零延迟、只有 2 个点、直线 —— 全是"看起来像机器"的真人形态
                BehaviorVerdict v = service.validateBehavior(issue.id(),
                        new BehaviorAnswer(String.valueOf(target),
                                List.of(new BehaviorAnswer.Point(0, 0, 0),
                                        new BehaviorAnswer.Point(target, 0, 1)), 1));
                assertTrue(v.passed(), () -> "宽容档下真人被误杀：" + v.detail());
            });
        }
    }

    @Nested
    @DisplayName("配置改了就生效")
    class ConfigActuallyWorks {

        @Test
        @DisplayName("调大 slider-tolerance 后，原本超出容差的偏移量能通过")
        void sliderToleranceConfigurable() {
            // 容差调到 100，偏移 60px 应该通过
            runner.withPropertyValues("easy-captcha.slider-tolerance=100")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                        int target = Integer.parseInt(issue.result().code());

                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(String.valueOf(target + 60), points(target + 60, 200), 400));
                        assertTrue(v.passed(), () -> "放宽容差后仍被拒：" + v.detail());
                    });
        }

        @Test
        @DisplayName("调小 slider-tolerance 后，原本能过的偏移量被拒")
        void tighterSliderToleranceRejects() {
            runner.withPropertyValues("easy-captcha.slider-tolerance=2")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                        int target = Integer.parseInt(issue.result().code());

                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(String.valueOf(target + 20), points(target + 20, 200), 400));
                        assertFalse(v.passed());
                        assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, v.reason());
                    });
        }

        @Test
        @DisplayName("显式设置 min-duration-ms 后，过快提交被拒")
        void minDurationConfigurable() {
            runner.withPropertyValues("easy-captcha.behavior.drag.min-duration-ms=10000")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                        int target = Integer.parseInt(issue.result().code());

                        // 服务端只经过了极短时间，远低于 10 秒
                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(String.valueOf(target), points(target, 100), 300));
                        assertFalse(v.passed(), "设置了下限就该拦住");
                        assertEquals(BehaviorVerdict.Reason.TOO_FAST, v.reason());
                    });
        }

        @Test
        @DisplayName("打开弯曲度检查后，直线拖动被拒")
        void curvatureCheckConfigurable() {
            runner.withPropertyValues("easy-captcha.behavior.drag.check-curvature=true",
                            "easy-captcha.behavior.drag.min-curvature=1.02")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                        int target = Integer.parseInt(issue.result().code());

                        // 完美直线
                        List<BehaviorAnswer.Point> straight = List.of(
                                new BehaviorAnswer.Point(0, 0, 0),
                                new BehaviorAnswer.Point(target / 2, 0, 200),
                                new BehaviorAnswer.Point(target, 0, 400),
                                new BehaviorAnswer.Point(target, 0, 500));

                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(String.valueOf(target), straight, 500));
                        assertFalse(v.passed(), "开启弯曲度检查后直线应被拒");
                        assertEquals(BehaviorVerdict.Reason.TOO_STRAIGHT, v.reason());
                    });
        }

        @Test
        @DisplayName("调小 max-duration-ms 后，挂机重放被拒")
        void maxDurationConfigurable() throws Exception {
            runner.withPropertyValues("easy-captcha.behavior.drag.max-duration-ms=100")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.SLIDER_CAPTCHA);
                        int target = Integer.parseInt(issue.result().code());

                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(String.valueOf(target), points(target, 100), 400));
                        assertFalse(v.passed(), "超过上限应被拒");
                        assertEquals(BehaviorVerdict.Reason.TIMEOUT, v.reason());
                    });
        }

        @Test
        @DisplayName("点击容差独立可配")
        void clickToleranceConfigurable() {
            runner.withPropertyValues("easy-captcha.click-tolerance=1")
                    .run(ctx -> {
                        CaptchaService service = ctx.getBean(CaptchaService.class);
                        CaptchaService.CaptchaIssue issue = service.issue(CaptchaTypeEnum.CLICK_WORD_CAPTCHA);

                        // 把正确答案的坐标整体平移 30px，容差只有 1px，必然不命中
                        String shifted = shift(issue.result().code(), 30);
                        BehaviorVerdict v = service.validateBehavior(issue.id(),
                                new BehaviorAnswer(shifted, points(120, 100), 400));
                        assertFalse(v.passed());
                        assertEquals(BehaviorVerdict.Reason.ANSWER_MISMATCH, v.reason());
                    });
        }

        private static String shift(String coords, int delta) {
            StringBuilder sb = new StringBuilder();
            for (String pair : coords.split(";")) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                String[] xy = pair.split(",");
                sb.append(Integer.parseInt(xy[0]) + delta).append(',').append(xy[1]);
            }
            return sb.toString();
        }
    }

    @Nested
    @DisplayName("预设档位")
    class Presets {

        @Test
        @DisplayName("lenient 与 strict 的差异符合预期")
        void presets() {
            BehaviorTuning lenient = BehaviorTuning.lenient();
            BehaviorTuning strict = BehaviorTuning.strict();

            assertEquals(0L, lenient.minDurationMs());
            assertEquals(1, lenient.minPoints());
            assertFalse(lenient.checkCurvature());

            // 严格模式也不设耗时下限，这是刻意的
            assertEquals(0L, strict.minDurationMs(), "严格模式也不应设置耗时下限");
            assertEquals(4, strict.minPoints());
            assertTrue(strict.checkCurvature());
            assertTrue(strict.monotonicTolerance() < lenient.monotonicTolerance(),
                    "严格模式的回退容差应更小");
        }

        @Test
        @DisplayName("BehaviorProperties 嵌套配置能正确转换")
        void nestedConversion() {
            BehaviorProperties props = new BehaviorProperties();
            props.getDrag().setMinPoints(5);
            props.getDrag().setMonotonicTolerance(15);
            props.getDrag().setCheckCurvature(true);
            props.getClick().setMinDurationMs(300);

            BehaviorTuning drag = props.dragTuning();
            assertEquals(5, drag.minPoints());
            assertEquals(15, drag.monotonicTolerance());
            assertTrue(drag.checkCurvature());

            BehaviorTuning click = props.clickTuning();
            assertEquals(300L, click.minDurationMs());
            // 点选的方向/弯曲度恒不参与判定
            assertEquals(Integer.MAX_VALUE, click.monotonicTolerance());
            assertFalse(click.checkCurvature());
        }

        @Test
        @DisplayName("非法配置在转换时被夹到安全范围，不会抛异常")
        void invalidValuesClamped() {
            BehaviorProperties props = new BehaviorProperties();
            props.getDrag().setMinDurationMs(-100);
            props.getDrag().setMinPoints(-5);
            props.getDrag().setMonotonicTolerance(-10);
            props.getDrag().setMinCurvature(0.5);

            BehaviorTuning drag = props.dragTuning();
            assertEquals(0L, drag.minDurationMs(), "负数应夹到 0");
            assertEquals(1, drag.minPoints(), "负数点数应夹到 1");
            assertEquals(0, drag.monotonicTolerance(), "负容差应夹到 0");
            assertEquals(1.0, drag.minCurvature(), "弯曲度下限不应小于 1.0");
        }
    }
}
