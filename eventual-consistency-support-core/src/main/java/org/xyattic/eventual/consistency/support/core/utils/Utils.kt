package org.xyattic.eventual.consistency.support.core.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author wangxing
 * @create 2020/11/16
 */
inline fun <reified T> T.getLogger(): Logger {
    if (T::class.isCompanion) {
        return LoggerFactory.getLogger(T::class.java.enclosingClass)
    }
    return LoggerFactory.getLogger(T::class.java)
}
