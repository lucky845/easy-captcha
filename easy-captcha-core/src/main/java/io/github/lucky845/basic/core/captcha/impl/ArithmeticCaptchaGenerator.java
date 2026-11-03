package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptchaGenerator;
import io.github.lucky845.basic.core.config.CaptchaConfig;
import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.result.CaptchaResult;
import io.github.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static io.github.lucky845.basic.core.constants.ArithmeticTypeConstants.ADDITION;
import static io.github.lucky845.basic.core.constants.ArithmeticTypeConstants.MULTIPLICATION;
import static io.github.lucky845.basic.core.constants.ArithmeticTypeConstants.SUBTRACTION;

/**
 * 算术验证码生成器。
 *
 * <p>返回的 {@code code} 是算式的**答案**，图片上绘制的是算式。
 * 表达式的答案在生成时按真实运算优先级计算，不做字符串拼接猜测。
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ArithmeticCaptchaGenerator extends AbstractCaptchaGenerator {

    private static final double FONT_RATIO = 0.6;

    private static final int MAX_RESULT = 100;

    private static final int MAX_ATTEMPTS = 100;

    @Override
    public CaptchaTypeEnum captchaType() {
        return CaptchaTypeEnum.ARITHMETIC_CAPTCHA;
    }

    @Override
    public ContentTypeEnum contentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaResult generate(CaptchaConfig config) {
        Problem problem = createProblem(config.difficulty());
        BufferedImage image = newImage(config);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);
            drawInterference(g, config);
            String expression = problem.expression() + " = ?";
            Font font = fitFont(g, expression, config, font(config, FONT_RATIO));
            drawCenteredText(g, expression, config, font);
        } finally {
            g.dispose();
        }
        return new CaptchaResult(String.valueOf(problem.answer()), encodePng(image), contentType());
    }

    /**
     * 算式与答案。
     */
    private record Problem(String expression, int answer) {
    }

    private Problem createProblem(ArithmeticDifficultyEnum difficulty) {
        ArithmeticDifficultyEnum level =
                difficulty == null ? ArithmeticDifficultyEnum.EASY : difficulty;
        return switch (level) {
            case EASY -> easy();
            case MEDIUM -> medium();
            case HARD -> hard();
            case EXPERT -> expert();
        };
    }

    /**
     * 一位数加减，结果非负。
     */
    private Problem easy() {
        int a = RandomUtils.nextIntInclusive(1, 9);
        int b = RandomUtils.nextIntInclusive(1, 9);
        if (RandomUtils.chance(0.5)) {
            return new Problem(a + " " + ADDITION + " " + b, a + b);
        }
        int max = Math.max(a, b);
        int min = Math.min(a, b);
        return new Problem(max + " " + SUBTRACTION + " " + min, max - min);
    }

    /**
     * 两位数加减一位数，结果始终可行。
     */
    private Problem medium() {
        int a = RandomUtils.nextIntInclusive(11, 50);
        int b = RandomUtils.nextIntInclusive(1, 9);
        if (RandomUtils.chance(0.5)) {
            return new Problem(a + " " + ADDITION + " " + b, a + b);
        }
        return new Problem(a + " " + SUBTRACTION + " " + b, a - b);
    }

    /**
     * 三个操作数、两个加减运算符，全程保持非负且不超过 {@value #MAX_RESULT}。
     */
    private Problem hard() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int a = RandomUtils.nextIntInclusive(2, 20);
            int b = RandomUtils.nextIntInclusive(2, 20);
            int c = RandomUtils.nextIntInclusive(2, 20);
            char op1 = RandomUtils.pick(new Character[]{ADDITION, SUBTRACTION});
            char op2 = RandomUtils.pick(new Character[]{ADDITION, SUBTRACTION});
            int step1 = op1 == ADDITION ? a + b : a - b;
            int result = op2 == ADDITION ? step1 + c : step1 - c;
            if (step1 >= 0 && result >= 0 && result <= MAX_RESULT) {
                return new Problem(a + " " + op1 + " " + b + " " + op2 + " " + c, result);
            }
        }
        return new Problem("1 " + ADDITION + " 1 " + ADDITION + " 1", 3);
    }

    /**
     * 先乘后加减，结果保持在 [0, {@value #MAX_RESULT}]。
     */
    private Problem expert() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int a = RandomUtils.nextIntInclusive(2, 9);
            int b = RandomUtils.nextIntInclusive(2, 9);
            int c = RandomUtils.nextIntInclusive(1, 20);
            boolean plus = RandomUtils.chance(0.5);
            int result = plus ? a * b + c : a * b - c;
            if (result >= 0 && result <= MAX_RESULT) {
                return new Problem(a + " " + MULTIPLICATION + " " + b + " "
                        + (plus ? ADDITION : SUBTRACTION) + " " + c, result);
            }
        }
        return new Problem("2 " + MULTIPLICATION + " 2 " + ADDITION + " 1", 5);
    }
}
