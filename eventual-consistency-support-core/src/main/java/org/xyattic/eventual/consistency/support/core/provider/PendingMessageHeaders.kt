package org.xyattic.eventual.consistency.support.core.provider

/**
 * @author wangxing
 * @create 2021/4/19
 */
class PendingMessageHeaders {

    companion object {

        @JvmStatic
        val partitionHeader = "partition";

        @JvmStatic
        val msgIdHeader = "msgId";

    }

}
