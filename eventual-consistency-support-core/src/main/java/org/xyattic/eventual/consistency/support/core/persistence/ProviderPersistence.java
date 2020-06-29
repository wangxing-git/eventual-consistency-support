package org.xyattic.eventual.consistency.support.core.persistence;

import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.util.Date;
import java.util.List;

/**
 * @author wangxing
 * @create 2020/4/14
 */
public interface ProviderPersistence {

    void save(List<PendingMessage> pendingMessages);

    void changePendingMessageStatus(String id, PendingMessageStatus status);

    List<PendingMessage> getPendingMessages(Date timeBefore);

}
