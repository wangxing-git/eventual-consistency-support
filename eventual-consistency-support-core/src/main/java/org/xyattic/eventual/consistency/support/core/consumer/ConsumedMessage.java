package org.xyattic.eventual.consistency.support.core.consumer;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Data
@Document("consumedMessages")
public class ConsumedMessage {

    @Id
    private String id;

    private Object message;

    private Boolean success;

    private String exception;

}