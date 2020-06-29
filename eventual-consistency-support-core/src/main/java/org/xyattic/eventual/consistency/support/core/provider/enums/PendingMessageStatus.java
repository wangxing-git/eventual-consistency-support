package org.xyattic.eventual.consistency.support.core.provider.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Getter
@AllArgsConstructor
public enum PendingMessageStatus {
    /**
     * 待发送
     */
    PENDING("10","待发送"),
    /**
     * 已发送
     */
    HAS_BEEN_SENT("20","已发送"),
    ;

    private String code;

    private String desc;

}
