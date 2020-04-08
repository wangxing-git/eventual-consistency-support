package org.xyattic.eventual.consistency.support.core.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author wangxing
 * @create 2020/4/2
 */
@Data
@AllArgsConstructor
public class TestMessage {

    private String eventId;

    private String name;

}