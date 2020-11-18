package org.xyattic.eventual.consistency.support.core.constants

/**
 * @author wangxing
 * @create 2020/6/29
 */
interface RabbitConstants {
    companion object {
        const val NAMESPACE = "RABBIT_"
        const val EXCHANGE_HEADER = NAMESPACE + "EXCHANGE"
        const val APP_ID_HEADER = NAMESPACE + "APP_ID"
    }
}