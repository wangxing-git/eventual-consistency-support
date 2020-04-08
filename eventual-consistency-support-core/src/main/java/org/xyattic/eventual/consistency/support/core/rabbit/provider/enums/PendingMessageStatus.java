package org.xyattic.eventual.consistency.support.core.rabbit.provider.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Getter
@AllArgsConstructor
public enum PendingMessageStatus {
    PENDING("10","待发送"),
    HAS_BEEN_SENT("20","已发送"),
    ;

    private String code;

    private String desc;

}
