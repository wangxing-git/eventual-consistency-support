package org.xyattic.eventual.consistency.support.core.exception;

/**
 * @author wangxing
 * @create 2020/4/8
 */
public class MqException extends RuntimeException {

    public MqException() {
    }

    public MqException(String message) {
        super(message);
    }

    public MqException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqException(Throwable cause) {
        super(cause);
    }

    public MqException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}