package org.xyattic.eventual.consistency.support.core.exception

/**
 * @author wangxing
 * @create 2020/4/8
 */
open class EventualConsistencyException : RuntimeException {
    constructor() {}
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean,
                writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace) {
    }

    companion object {
        private const val serialVersionUID: Long = 7466614818862010797L
    }

}