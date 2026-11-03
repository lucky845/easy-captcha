package io.github.lucky845.basic.core.behavior;

/**
 * 行为校验结果。
 *
 * <p>除了"过没过"，还要告诉调用方**为什么没过**——否则线上出现批量失败时，
 * 根本分不清是攻击还是自己的前端有 bug。
 *
 * @param passed 是否通过
 * @param reason 结果原因
 * @param detail 补充说明，便于排查
 * @author created by lucky845
 */
public record BehaviorVerdict(boolean passed, Reason reason, String detail) {

    public static BehaviorVerdict pass() {
        return new BehaviorVerdict(true, Reason.OK, "校验通过");
    }

    public static BehaviorVerdict reject(Reason reason, String detail) {
        return new BehaviorVerdict(false, reason, detail);
    }

    /**
     * 未通过的原因
     */
    public enum Reason {
        /**
         * 通过
         */
        OK,
        /**
         * 答案（偏移量/坐标）不匹配
         */
        ANSWER_MISMATCH,
        /**
         * 操作过快，疑似脚本
         */
        TOO_FAST,
        /**
         * 操作超时
         */
        TIMEOUT,
        /**
         * 轨迹点太少
         */
        TOO_FEW_POINTS,
        /**
         * 轨迹方向异常（来回大幅度跳动）
         */
        ABNORMAL_DIRECTION,
        /**
         * 轨迹过于笔直，疑似脚本生成
         */
        TOO_STRAIGHT,
        /**
         * 输入为空或格式非法
         */
        INVALID_INPUT
    }
}
