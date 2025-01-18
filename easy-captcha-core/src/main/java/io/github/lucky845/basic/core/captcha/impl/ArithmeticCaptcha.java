package io.github.lucky845.basic.core.captcha.impl;

import io.github.lucky845.basic.core.captcha.AbstractCaptcha;
import io.github.lucky845.basic.core.constants.ArithmeticTypeConstants;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.enums.ArithmeticDifficultyEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static io.github.lucky845.basic.core.utils.RandomUtils.num;

/**
 * 算数类型验证码
 *
 * @author created by lucky845 on 2025-01-16
 */
public class ArithmeticCaptcha extends AbstractCaptcha {

    private ArithmeticDifficultyEnum difficultyEnum = ArithmeticDifficultyEnum.EASY; // 默认难度级别
    private static final int MAX_RESULT = 100; // 最大结果限制

    /**
     * 设置难度级别
     */
    public void setDifficulty(ArithmeticDifficultyEnum difficulty) {
        this.difficultyEnum = difficulty;
    }

    /**
     * 生成算数表达式
     */
    private String generateArithmeticExpression() {
        Expression expression = switch (difficultyEnum) {
            case EASY -> generateEasyExpression();
            case MEDIUM -> generateMediumExpression();
            case HARD -> generateHardExpression();
            case EXPERT -> generateExpertExpression();
        };

        this.captcha = String.valueOf(expression.result());
        return expression.text();
    }

    /**
     * 生成简单表达式 (个位数)
     */
    private Expression generateEasyExpression() {
        int num1 = num(1, 9);
        int num2 = num(1, 9);
        char operator = getRandomOperator();

        // 特殊处理除法，确保能够整除
        if (operator == ArithmeticTypeConstants.DIVISION) {
            num1 = num2 * num(1, 9); // 确保 num1 能被 num2 整除
        }

        int result = calculateResult(num1, num2, operator);
        // 确保结果为正数且不超过限制
        while (result < 0 || result > MAX_RESULT || (operator == ArithmeticTypeConstants.DIVISION && num1 % num2 != 0)) {
            num1 = num(1, 9);
            num2 = num(1, 9);
            if (operator == ArithmeticTypeConstants.DIVISION) {
                num1 = num2 * num(1, 9);
            }
            result = calculateResult(num1, num2, operator);
        }

        return new Expression(
                String.format("%d %c %d = ?", num1, operator, num2),
                result
        );
    }

    /**
     * 生成中等难度表达式 (两位数)
     */
    private Expression generateMediumExpression() {
        int num1 = num(10, 99);
        int num2 = num(1, 20);
        char operator = getRandomOperator();

        // 特殊处理除法，确保能够整除
        if (operator == ArithmeticTypeConstants.DIVISION) {
            num1 = num2 * num(1, 9); // 确保 num1 能被 num2 整除
        }

        int result = calculateResult(num1, num2, operator);
        // 确保结果合理
        while (result < 0 || result > MAX_RESULT || (operator == ArithmeticTypeConstants.DIVISION && num1 % num2 != 0)) {
            num1 = num(10, 99);
            num2 = num(1, 20);
            if (operator == ArithmeticTypeConstants.DIVISION) {
                num1 = num2 * num(1, 9);
            }
            result = calculateResult(num1, num2, operator);
        }

        return new Expression(
                String.format("%d %c %d = ?", num1, operator, num2),
                result
        );
    }

    /**
     * 生成困难表达式 (多运算符)
     */
    private Expression generateHardExpression() {
        List<Integer> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();

        // 生成3个数字和2个运算符
        for (int i = 0; i < 3; i++) {
            numbers.add(num(1, 9));
        }
        for (int i = 0; i < 2; i++) {
            operators.add(getRandomOperator());
        }

        // 计算结果
        int result = calculateMultipleOperations(numbers, operators);
        // 重新生成直到得到合适的结果
        while (result < 0 || result > MAX_RESULT) {
            numbers.clear();
            for (int i = 0; i < 3; i++) {
                numbers.add(num(1, 9));
            }
            result = calculateMultipleOperations(numbers, operators);
        }

        return new Expression(
                String.format("%d %c %d %c %d = ?",
                        numbers.get(0), operators.get(0),
                        numbers.get(1), operators.get(1),
                        numbers.get(2)),
                result
        );
    }

    /**
     * 生成专家级表达式 (带括号)
     */
    private Expression generateExpertExpression() {
        int num1 = num(1, 9);
        int num2 = num(1, 9);
        int num3 = num(1, 9);
        char op1 = getRandomOperator();
        char op2 = getRandomOperator();

        // 随机决定括号位置
        boolean leftBracket = num(2) == 0;

        int result;
        String expression;
        if (leftBracket) {
            // (a op1 b) op2 c
            int bracketResult = calculateResult(num1, num2, op1);
            result = calculateResult(bracketResult, num3, op2);
            expression = String.format("(%d %c %d) %c %d = ?", num1, op1, num2, op2, num3);
        } else {
            // a op1 (b op2 c)
            int bracketResult = calculateResult(num2, num3, op2);
            result = calculateResult(num1, bracketResult, op1);
            expression = String.format("%d %c (%d %c %d) = ?", num1, op1, num2, op2, num3);
        }

        // 重新生成直到得到合适的结果
        while (result < 0 || result > MAX_RESULT) {
            return generateHardExpression(); // 如果结果不合适，退化为困难级别
        }

        return new Expression(expression, result);
    }

    /**
     * 获取随机运算符
     */
    private char getRandomOperator() {
        char[] operators = {ArithmeticTypeConstants.ADDITION, ArithmeticTypeConstants.SUBTRACTION,
                ArithmeticTypeConstants.MULTIPLICATION, ArithmeticTypeConstants.DIVISION};
        return operators[num(operators.length)];
    }

    /**
     * 计算两个数的结果
     */
    private int calculateResult(int num1, int num2, char operator) {
        return switch (operator) {
            case ArithmeticTypeConstants.ADDITION -> num1 + num2;
            case ArithmeticTypeConstants.SUBTRACTION -> num1 - num2;
            case ArithmeticTypeConstants.MULTIPLICATION -> num1 * num2;
            case ArithmeticTypeConstants.DIVISION -> num1 / num2;
            default -> throw new IllegalArgumentException("不支持的运算符: " + operator);
        };
    }

    /**
     * 计算多个运算符的结果
     */
    private int calculateMultipleOperations(List<Integer> numbers, List<Character> operators) {
        // 先处理乘法
        int result = numbers.getFirst();
        for (int i = 0; i < operators.size(); i++) {
            if (operators.get(i) == ArithmeticTypeConstants.MULTIPLICATION) {
                result = calculateResult(result, numbers.get(i + 1), ArithmeticTypeConstants.MULTIPLICATION);
            }
        }

        // 再处理加减法
        for (int i = 0; i < operators.size(); i++) {
            if (operators.get(i) != ArithmeticTypeConstants.MULTIPLICATION) {
                result = calculateResult(result, numbers.get(i + 1), operators.get(i));
            }
        }

        return result;
    }

    @Override
    public boolean createCaptcha(OutputStream out) throws CaptchaGeneratorException {
        BufferedImage image = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置渲染品质
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, captchaWidth, captchaHeight);

        // 绘制算数表达式
        String expression = generateArithmeticExpression();
        g2d.setColor(Color.BLACK);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 28f));

        // 计算文本位置使其居中
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (captchaWidth - metrics.stringWidth(expression)) / 2;
        int y = ((captchaHeight - metrics.getHeight()) / 2) + metrics.getAscent();

        g2d.drawString(expression, x, y);

        // 添加干扰元素
        drawLine(3, randomColor(), g2d);
        drawOval(2, randomColor(), g2d);

        g2d.dispose();

        try {
            return ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new CaptchaGeneratorException("生成算数验证码失败", e);
        }
    }

    @Override
    public ContentTypeEnum getContentType() {
        return ContentTypeEnum.PNG;
    }

    @Override
    public CaptchaTypeEnum getCaptchaType() {
        return CaptchaTypeEnum.ARITHMETIC_CAPTCHA;
    }

    /**
     * 表达式记录类
     */
    private record Expression(String text, int result) {
    }
}
