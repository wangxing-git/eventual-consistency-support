package org.xyattic.eventual.consistency.support.core.exception

/**
 * @author wangxing
 * @create 2021/4/15
 */
open class DuplicateMessageException : MqException {

    constructor() {}
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(cause: Throwable?) : super(cause) {}
    constructor(
        message: String?, cause: Throwable?, enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace) {
    }

}
