package org.xyattic.eventual.consistency.support.example.provider.controller;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyattic.eventual.consistency.support.core.constants.RabbitConstants;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageContextHolder;
import org.xyattic.eventual.consistency.support.core.provider.aop.SendMqMessage;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Component
@RestController
public class TestListener {

    @SendMqMessage
    @Transactional(rollbackFor = Exception.class)
    @GetMapping("/guest/testSend")
    public List<PendingMessage> testSend() throws Exception {
//        String id="f76c6ef9-9c4a-4b5e-a786-a947cf554092";
        String id = UUID.randomUUID().toString();
        TestMessage testMessage = new TestMessage(id, "ffs");

        final PendingMessage pendingMessage =
                PendingMessage.builder(id, testMessage, "test-k")
                        .build();
        pendingMessage.getHeaders().put(RabbitConstants.APP_ID_HEADER, "appid");
        pendingMessage.getHeaders().put(RabbitConstants.EXCHANGE_HEADER, "test-e");
        PendingMessageContextHolder.add(pendingMessage);
//        PendingMessageContextHolder.addAll(Arrays.asList(
//                PendingMessage.builder()
//                        .appId("appid")
//                        .body(testMessage)
//                        .exchange("test-e")
//                        .routingKey("test-k")
//                        .messageId(id)
//                        .build()
//        ));
//        PendingMessageContextHolder.add(
//                PendingMessage.builder()
//                        .appId("appid")
//                        .body(testMessage)
//                        .exchange("test-e")
//                        .routingKey("test-k")
//                        .messageId(id)
//                        .build()
//        );
        return Collections.singletonList(pendingMessage);
    }

}