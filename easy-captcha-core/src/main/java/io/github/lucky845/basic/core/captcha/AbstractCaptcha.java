package io.github.lucky845.basic.core.captcha;

import io.github.lucky845.basic.core.constants.Constants;
import io.github.lucky845.basic.core.enums.CaptchaTypeEnum;
import io.github.lucky845.basic.core.enums.CharTypeEnum;
import io.github.lucky845.basic.core.enums.ColorEnum;
import io.github.lucky845.basic.core.enums.ContentTypeEnum;
import io.github.lucky845.basic.core.enums.FontEnum;
import io.github.lucky845.basic.core.exception.CaptchaGeneratorException;
import io.github.lucky845.basic.core.utils.RandomUtils;
import lombok.Data;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Logger;

import static io.github.lucky845.basic.core.utils.RandomUtils.chars;
import static io.github.lucky845.basic.core.utils.RandomUtils.num;

/**
 * 验证码抽象类
 *
 * @author created by lucky845 on 2025-01-16
 */
@Data
public abstract class AbstractCaptcha implements Captcha {

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * 验证码
     */
    protected String captcha;

    /**
     * 字体类型
     */
    protected Font font;

    /**
     * 验证码字符类型
     */
    protected CharTypeEnum charType;

    /**
     * 验证码长度
     */
    protected int captchaLength;

    /**
     * 验证码显示宽度
     */
    protected int captchaWidth;

    /**
     * 验证码显示高度
     */
    protected int captchaHeight;

    /**
     * 获取图片类型
     *
     * @return 图片格式，MIME类型
     */
    protected abstract ContentTypeEnum getContentType();

    /**
     * 获取验证码类型
     */
    protected abstract CaptchaTypeEnum getCaptchaType();

    /**
     * 给定范围获得随机颜色
     *
     * @param fc 0-255
     * @param bc 0-255
     * @return 随机颜色
     */
    protected Color randomColor(int fc, int bc) {
        if (fc > 255) {
            fc = 255;
        }
        if (bc > 255) {
            bc = 255;
        }
        int r = fc + num(bc - fc);
        int g = fc + num(bc - fc);
        int b = fc + num(bc - fc);
        return new Color(r, g, b);
    }

    /**
     * 获取随机常用颜色
     *
     * @return 随机颜色
     */
    protected Color randomColor() {
        return ColorEnum.getRandomColor();
    }

    @Override
    public String createBase64Captcha() {
        return toBase64();
    }

    /**
     * 输出base64编码
     *
     * @return base64编码字符串
     */
    protected String toBase64() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean success = createCaptcha(outputStream);
        if (!success) {
            throw new CaptchaGeneratorException();
        }
        return getContentType().getBase64Prefix()
                + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public Font getFont() {
        if (font == null) {
            try {
                setFont(FontEnum.ACTIONJ);
            } catch (Exception e) {
                setFont(new Font("Arial", Font.BOLD, 32));
            }
        }
        return font;
    }

    /**
     * 设置字体
     *
     * @param font 字体
     */
    public void setFont(Font font) {
        this.font = font;
    }

    public void setFont(FontEnum font) throws IOException, FontFormatException {
        setFont(font, 32f);
    }

    public void setFont(FontEnum font, float size) throws IOException, FontFormatException {
        setFont(font, Font.BOLD, size);
    }

    public void setFont(FontEnum font, int style, float size) throws IOException, FontFormatException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/" + font.getPath());
        assert resourceAsStream != null;
        this.font = Font.createFont(Font.TRUETYPE_FONT, resourceAsStream)
                .deriveFont(style, size);
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public void setCaptcha(char[] captcha) {
        this.captcha = new String(captcha);
    }

    /**
     * 生成验证码
     */
    protected String generateCaptcha() {
        return switch (getCaptchaType()) {
            case CaptchaTypeEnum.CHINESE_CAPTCHA, CaptchaTypeEnum.CHINESE_GIF_CAPTCHA -> generateChineseCaptcha();
            default -> generateRandomCaptcha();
        };
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < captchaLength; i++) {
            switch (charType) {
                case PURE_NUMBER -> sb.append(chars(Constants.NUM_MAX_INDEX));
                case PURE_LETTERS -> sb.append(chars(Constants.CHAR_MIN_INDEX, Constants.CHAR_MAX_INDEX));
                case PURE_CAPITAL_LETTERS -> sb.append(chars(Constants.UPPER_MIN_INDEX, Constants.UPPER_MAX_INDEX));
                case PURE_LOWERCASE_LETTERS -> sb.append(chars(Constants.LOWER_MIN_INDEX, Constants.LOWER_MAX_INDEX));
                case ALPHANUMERIC_MIX -> sb.append(chars(Constants.UPPER_MAX_INDEX));
                default -> sb.append(chars());
            }
        }
        String captcha = sb.toString();
        setCaptcha(captcha);
        return captcha;
    }

    /**
     * 生成随机中文字符
     *
     * @return 中文字符串
     */
    private String generateChineseCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < captchaLength; i++) {
            int index = RandomUtils.num(Constants.DELTA.length());
            char ch = Constants.DELTA.charAt(index);
            sb.append(ch);
        }
        String captcha = sb.toString();
        setCaptcha(captcha);
        return captcha;
    }

    /**
     * 绘制中文验证码
     */
    protected void drawChineseCaptcha(Graphics2D g2d) {
        // 定义字符之间的安全间距
        int padding = 10; // 可以根据需要调整

        // 设置字体
        Font font = new Font("宋体", Font.BOLD, 32);
        g2d.setFont(font);

        // 获取字体度量
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int charHeight = fontMetrics.getHeight(); // 字符高度
        int charWidth = fontMetrics.charWidth('汉'); // 字符宽度（以“汉”为例）

        // 字符之间的水平间距
        int horizontalSpacing = 10;

        // 生成验证码
        String chineseCaptcha = generateCaptcha();
        char[] charArray = chineseCaptcha.toCharArray();

        // 计算总宽度
        int totalWidth = charArray.length * (charWidth + horizontalSpacing) - horizontalSpacing;

        // 起始绘制位置（水平居中）
        int startX = (captchaWidth - totalWidth) / 2;

        for (int i = 0; i < charArray.length; i++) {
            char ch = charArray[i];

            // 计算当前字符的水平位置
            int x = startX + i * (charWidth + horizontalSpacing);

            // 计算垂直位置的范围
            int minY = charHeight + padding; // 最小垂直位置
            int maxY = captchaHeight - padding; // 最大垂直位置

            // 检查参数是否合法
            if (minY >= maxY) {
                // 如果最小垂直位置大于等于最大垂直位置，调整范围
                minY = charHeight;
                maxY = captchaHeight;
            }

            // 随机生成垂直位置（在允许范围内）
            int y = num(minY, maxY);

            // 配置颜色
            g2d.setColor(randomColor());

            // 设置字体旋转角度（限制在 -15° 到 15° 之间）
            int degree = new Random().nextInt(30) - 15;

            // 顺时针旋转
            g2d.rotate(degree * Math.PI / 180, x, y);

            // 开始画汉字
            g2d.drawString(ch + "", x, y);

            // 逆时针旋转
            g2d.rotate(-(degree * Math.PI / 180), x, y);
        }
    }

    /**
     * 获取当前的验证码
     *
     * @return 字符串
     */
    public String getCaptcha() {
        checkCaptcha();
        return captcha;
    }

    /**
     * 检查验证码是否生成，没有则立即生成
     */
    public void checkCaptcha() {
        if (captcha == null) {
            // 生成验证码
            String captcha = generateCaptcha();
            setCaptcha(captcha);
        }
    }

    /**
     * 随机画干扰线
     *
     * @param num 数量
     * @param g   Graphics2D
     */
    public void drawLine(int num, Graphics2D g) {
        drawLine(num, null, g);
    }

    /**
     * 随机画干扰线
     *
     * @param num   数量
     * @param color 颜色
     * @param g     Graphics2D
     */
    public void drawLine(int num, Color color, Graphics2D g) {
        for (int i = 0; i < num; i++) {
            g.setColor(color == null ? randomColor() : color);
            int x1 = num(-10, captchaWidth - 10);
            int y1 = num(5, captchaHeight - 5);
            int x2 = num(10, captchaWidth + 10);
            int y2 = num(2, captchaHeight - 2);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 随机画干扰圆
     *
     * @param num 数量
     * @param g   Graphics2D
     */
    public void drawOval(int num, Graphics2D g) {
        drawOval(num, null, g);
    }

    /**
     * 随机画干扰圆
     *
     * @param num   数量
     * @param color 颜色
     * @param g     Graphics2D
     */
    public void drawOval(int num, Color color, Graphics2D g) {
        for (int i = 0; i < num; i++) {
            g.setColor(color == null ? randomColor() : color);
            int w = 5 + num(10);
            g.drawOval(
                    num(captchaWidth - 25),
                    num(captchaHeight - 15),
                    w,
                    w
            );
        }
    }

    /**
     * 随机画贝塞尔曲线
     *
     * @param num 数量
     * @param g   Graphics2D
     */
    public void drawBesselLine(int num, Graphics2D g) {
        drawBesselLine(num, null, g);
    }

    /**
     * 随机画贝塞尔曲线
     *
     * @param num   数量
     * @param color 颜色
     * @param g     Graphics2D
     */
    public void drawBesselLine(int num, Color color, Graphics2D g) {
        for (int i = 0; i < num; i++) {
            g.setColor(color == null ? randomColor() : color);
            int x1 = 5, y1 = num(5, captchaHeight / 2);
            int x2 = captchaWidth - 5, y2 = num(captchaHeight / 2, captchaHeight - 5);
            int ctrlx = num(captchaWidth / 4, captchaWidth / 4 * 3), ctrly = num(5, captchaHeight - 5);
            if (num(2) == 0) {
                int ty = y1;
                y1 = y2;
                y2 = ty;
            }
            if (num(2) == 0) {  // 二阶贝塞尔曲线
                QuadCurve2D shape = new QuadCurve2D.Double();
                shape.setCurve(x1, y1, ctrlx, ctrly, x2, y2);
                g.draw(shape);
            } else {  // 三阶贝塞尔曲线
                int ctrlx1 = num(captchaWidth / 4, captchaWidth / 4 * 3), ctrly1 = num(5, captchaHeight - 5);
                CubicCurve2D shape = new CubicCurve2D.Double(x1, y1, ctrlx, ctrly, ctrlx1, ctrly1, x2, y2);
                g.draw(shape);
            }
        }
    }

}
