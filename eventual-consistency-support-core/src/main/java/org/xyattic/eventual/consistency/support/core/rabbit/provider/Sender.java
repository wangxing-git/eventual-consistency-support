package org.xyattic.eventual.consistency.support.core.rabbit.provider;

import java.util.List;

/**
 * @author wangxing
 * @create 2020/4/1
 */
public interface Sender {

    void send(PendingMessage pendingMessage);

    void send(List<PendingMessage> pendingMessage);

}
