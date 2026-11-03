package io.github.lucky845.basic.core.result;

import io.github.lucky845.basic.core.enums.ContentTypeEnum;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * 一次验证码生成的完整结果：正确答案 + 图片字节 + 内容类型 + 附加数据。
 *
 * <p>结果对象不可变，并且把"答案"和"图片"绑定在同一份数据里，
 * 调用方不需要再从某个共享字段里二次读取，天然避免并发串号。
 *
 * <p>{@code extra} 用于承载前端渲染行为验证码需要的公开信息，
 * 例如滑块验证码的背景图与拼图块。它**只包含可以公开给浏览器的数据**，
 * 真正的答案始终在 {@link #code} 里，不会出现在 {@code extra} 中。
 *
 * @param code        验证码答案（算术验证码为计算结果；滑块为水平偏移量；点选为坐标串）
 * @param image       图片二进制（主图）
 * @param contentType 图片 MIME 类型
 * @param extra       前端渲染用的附加数据，可能为空 Map
 * @author created by lucky845 on 2025-01-16
 */
public record CaptchaResult(String code, byte[] image, ContentTypeEnum contentType, Map<String, Object> extra) {

    public CaptchaResult {
        Objects.requireNonNull(code, "code 不能为空");
        Objects.requireNonNull(image, "image 不能为空");
        Objects.requireNonNull(contentType, "contentType 不能为空");
        if (image.length == 0) {
            throw new IllegalArgumentException("image 不能为空字节数组");
        }
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    /**
     * 兼容旧用法的三参构造，无附加数据。
     */
    public CaptchaResult(String code, byte[] image, ContentTypeEnum contentType) {
        this(code, image, contentType, Map.of());
    }

    /**
     * 图片的 MIME 类型，例如 {@code image/png}。
     */
    public String mimeType() {
        return contentType.getContentType();
    }

    /**
     * 返回 {@code data:image/xxx;base64,...} 形式，可直接放进 img 的 src。
     */
    public String toBase64() {
        return contentType.getBase64Prefix() + Base64.getEncoder().encodeToString(image);
    }

    /**
     * 与 {@link #toBase64()} 相同，语义更直观的别名。
     */
    public String toDataUri() {
        return toBase64();
    }

    /**
     * 图片字节数。
     */
    public int size() {
        return image.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaptchaResult that)) {
            return false;
        }
        return code.equals(that.code)
                && contentType == that.contentType
                && extra.equals(that.extra)
                && Arrays.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + contentType.hashCode();
        result = 31 * result + extra.hashCode();
        result = 31 * result + Arrays.hashCode(image);
        return result;
    }

    @Override
    public String toString() {
        return "CaptchaResult[code=" + code + ", contentType=" + contentType
                + ", bytes=" + image.length + ", extraKeys=" + extra.keySet() + "]";
    }
}
