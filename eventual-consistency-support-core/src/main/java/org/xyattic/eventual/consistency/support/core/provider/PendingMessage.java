package org.xyattic.eventual.consistency.support.core.provider;

import com.google.common.collect.Maps;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.messaging.MessageHeaders;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.io.Serializable;
import java.util.Date;

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

    private Object body;

    private String destination;

    private MessageHeaders headers;

    @Builder.Default
    private PendingMessageStatus status = PendingMessageStatus.PENDING;

    private String persistenceName;

    private String transactionManager;

    private Date createTime;

    public static class PendingMessageBuilder {

        private MessageHeaders headers = new MessageHeaders(Maps.newLinkedHashMap());

        public PendingMessageBuilder setHeader(String header, Object value) {
            this.headers.put(header, value);
            return this;
        }

    }

}