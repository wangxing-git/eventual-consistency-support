package org.xyattic.eventual.consistency.support.core.rabbit.provider;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.enums.PendingMessageStatus;

import java.io.Serializable;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Data
@Builder
@Document("pendingMessages")
public class PendingMessage implements Serializable {

    @Id
    private String messageId;

    private String appId;

    private Object body;

    private String exchange;

    private String routingKey;

    @Builder.Default
    private PendingMessageStatus status = PendingMessageStatus.PENDING;

    private String mongoTemplate;

    private String transactionManager;

}