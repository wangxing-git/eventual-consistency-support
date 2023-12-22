package org.xyattic.eventual.consistency.support.core.exception

/**
 * @author wangxing
 * @create 2021/4/15
 */
open class ConsumeMessageException : EventualConsistencyException {

    var reconsume = true

    constructor() {}
    constructor(message: String?) : super(message) {}
    constructor(message: String?, reconsume: Boolean) : super(message) {
        this.reconsume = reconsume
    }

    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(cause: Throwable?) : super(cause) {}
    constructor(cause: Throwable?, reconsume: Boolean) : super(cause) {
        this.reconsume = reconsume
    }

    constructor(message: String?, cause: Throwable?, reconsume: Boolean) : super(message, cause) {
        this.reconsume = reconsume
    }

    constructor(
        message: String?, cause: Throwable?, enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace) {
    }

}
