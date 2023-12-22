package org.xyattic.eventual.consistency.support.example.consumer.rabbit.listener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xyattic.eventual.consistency.support.core.consumer.AbstractMessage;

/**
 * @author wangxing
 * @create 2020/4/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestMessage extends AbstractMessage {

    private String id;

    private String eventId;

    private String name;

}