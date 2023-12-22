package org.xyattic.eventual.consistency.support.core.provider.enums

/**
 * @author wangxing
 * @create 2020/4/1
 */
enum class PendingMessageStatus(
    val code: String,
    val desc: String
) {
    /**
     * 待发送
     */
    PENDING("10", "待发送"),

    /**
     * 已发送
     */
    HAS_BEEN_SENT("20", "已发送"),

    /**
     * 发送失败
     */
    FAILED_TO_SEND("30", "发送失败"),
    ;

}