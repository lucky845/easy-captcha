package com.lucky845.basic.core.captcha;

import com.lucky845.basic.core.constants.Constants;
import com.lucky845.basic.core.enums.CharTypeEnum;
import com.lucky845.basic.core.enums.ColorEnum;
import com.lucky845.basic.core.enums.FontEnum;
import com.lucky845.basic.core.exception.CaptchaGeneratorException;
import com.lucky845.basic.core.utils.RandomUtils;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.logging.Logger;

import static com.lucky845.basic.core.utils.RandomUtils.chars;
import static com.lucky845.basic.core.utils.RandomUtils.num;

/**
 * 验证码抽象类
 *
 * @author created by lucky845 on 2025-01-16
 */
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
     * 验证码类型
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

    public AbstractCaptcha() {

    }

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

    public void setCharType(int type) {
        this.charType = CharTypeEnum.fromType(type);
    }

    /**
     * 设置字体
     *
     * @param font 字体
     */
    public void setFont(Font font) {
        this.font = font;
    }

    public void setFont(int type) throws IOException, FontFormatException {
        setFont(FontEnum.fromType(type));
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
     * 生成随机验证码
     */
    protected char[] randomCaptcha() {
        char[] captcha = new char[captchaLength];
        for (int i = 0; i < captchaLength; i++) {
            switch (charType) {
                case TYPE_ONLY_NUMBER -> captcha[i] = chars(Constants.NUM_MAX_INDEX);
                case TYPE_ONLY_CAPTCHA -> captcha[i] = chars(Constants.CHAR_MIN_INDEX, Constants.CHAR_MAX_INDEX);
                case TYPE_ONLY_CAPTCHA_NUM -> captcha[i] = chars(Constants.UPPER_MIN_INDEX, Constants.UPPER_MAX_INDEX);
                case TYPE_ONLY_CAPTCHA_NUM_NUM ->
                        captcha[i] = chars(Constants.LOWER_MIN_INDEX, Constants.LOWER_MAX_INDEX);
                case TYPE_ONLY_CAPTCHA_NUM_NUM_NUM -> captcha[i] = chars(Constants.UPPER_MAX_INDEX);
                default -> captcha[i] = chars();
            }
        }
        return captcha;
    }

    /**
     * 生成随机中文字符
     *
     * @return 中文字符串
     */
    protected String generateChineseCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < captchaLength; i++) {
            int index = RandomUtils.num(Constants.DELTA.length());
            char ch = Constants.DELTA.charAt(index);
            sb.append(ch);
        }
        return sb.toString();
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
     * 获取当前验证码的字符数组
     *
     * @return 字符数组
     */
    public char[] captchaChars() {
        checkCaptcha();
        return captcha.toCharArray();
    }

    /**
     * 检查验证码是否生成，没有则立即生成
     */
    public void checkCaptcha() {
        if (captcha == null) {
            // 生成验证码
            char[] captcha = randomCaptcha();
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

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public CharTypeEnum getCharType() {
        return charType;
    }

    public void setCharType(CharTypeEnum charType) {
        this.charType = charType;
    }

    public int getCaptchaLength() {
        return captchaLength;
    }

    public void setCaptchaLength(int captchaLength) {
        this.captchaLength = captchaLength;
    }

    public int getCaptchaWidth() {
        return captchaWidth;
    }

    public void setCaptchaWidth(int captchaWidth) {
        this.captchaWidth = captchaWidth;
    }

    public int getCaptchaHeight() {
        return captchaHeight;
    }

    public void setCaptchaHeight(int captchaHeight) {
        this.captchaHeight = captchaHeight;
    }
}
