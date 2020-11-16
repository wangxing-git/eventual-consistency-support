package org.xyattic.eventual.consistency.support.core.provider;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author wangxing
 * @create 2020/4/1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("pendingMessages")
public class PendingMessage implements Serializable {

    @Id
    @NonNull
    private String messageId;

    @NonNull
    private Object body;

    @NonNull
    private String destination;

    private Map<String, Object> headers;

    @Builder.Default
    private PendingMessageStatus status = PendingMessageStatus.PENDING;

    private String persistenceName;

    private String transactionManager;

    private Date createTime;

    public static class PendingMessageBuilder {

        private Map<String, Object> headers = Maps.newLinkedHashMap();

        public PendingMessageBuilder setHeader(String header, Object value) {
            this.headers.put(header, value);
            return this;
        }

    }

}