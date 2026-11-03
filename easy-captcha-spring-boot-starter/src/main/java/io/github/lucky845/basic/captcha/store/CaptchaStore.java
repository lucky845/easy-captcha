package io.github.lucky845.basic.captcha.store;

import io.github.lucky845.basic.core.result.CaptchaResult;

import java.util.Optional;

/**
 * 验证码存储，负责在"生成"和"校验"之间保存答案与签发时刻。
 *
 * <p>实现必须是线程安全的；{@link #remove(String)} 语义为"取出并删除"，
 * 保证同一个验证码只能被校验一次。
 *
 * @author created by lucky845 on 2025-01-16
 */
public interface CaptchaStore {

    /**
     * 保存一份验证码。
     *
     * @param id     验证码 id
     * @param result 验证码结果
     */
    void save(String id, CaptchaResult result);

    /**
     * 取出并删除一份验证码，过期或不存在时返回 {@link Optional#empty()}。
     *
     * @param id 验证码 id
     */
    Optional<CaptchaResult> remove(String id);

    /**
     * 取出并删除，同时带回服务端记录的签发时刻。
     *
     * <p>签发时刻由服务端时钟写入，客户端无法伪造。它是判断"用户到底花了多久"
     * 的唯一可信来源——客户端上报的 duration 可以被脚本任意填写。
     *
     * @param id 验证码 id
     * @return 结果与签发时刻；不存在或已过期时为空
     */
    default Optional<Issued> take(String id) {
        long now = System.currentTimeMillis();
        return remove(id).map(result -> new Issued(result, now));
    }

    /**
     * 当前有效条数。
     */
    int size();

    /**
     * 清空全部验证码。
     */
    void clear();

    /**
     * 取出结果：验证码内容 + 服务端签发时刻。
     *
     * @param result    验证码结果
     * @param issuedAt  签发时刻（毫秒时间戳，服务端时钟）
     */
    record Issued(CaptchaResult result, long issuedAt) {
    }
}
