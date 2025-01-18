package io.github.lucky845.basic.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片类型枚举
 *
 * @author created by lucky845 on 2025-01-16
 */
@Getter
@AllArgsConstructor
public enum ContentTypeEnum {

    PNG("image/png", "data:image/png;base64,"), // PNG 图片类型
    GIF("image/gif", "data:image/gif;base64,"), // GIF 图片类型
    JPG("image/jpeg", "data:image/jpeg;base64,"), // JPG 图片类型
    JPEG("image/jpeg", "data:image/jpeg;base64,"), // JPEG 图片类型
    BMP("image/bmp", "data:image/bmp;base64,"), // BMP 图片类型
    WEBP("image/webp", "data:image/webp;base64,"),
    ; // WEBP 图片类型

    /**
     * 图片的 MIME 类型
     */
    private final String contentType;

    /**
     * Base64 编码的前缀
     */
    private final String base64Prefix;
}
