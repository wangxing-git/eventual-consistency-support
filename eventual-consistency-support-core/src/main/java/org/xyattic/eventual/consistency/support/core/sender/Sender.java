package org.xyattic.eventual.consistency.support.core.sender;

import org.springframework.util.CollectionUtils;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;

import java.util.List;

/**
 * @author wangxing
 * @create 2020/4/1
 */
public interface Sender {

    void send(PendingMessage pendingMessage);

    default void send(List<PendingMessage> pendingMessages) {
        if (!CollectionUtils.isEmpty(pendingMessages)) {
            pendingMessages.forEach(this::send);
        }
    }

}
