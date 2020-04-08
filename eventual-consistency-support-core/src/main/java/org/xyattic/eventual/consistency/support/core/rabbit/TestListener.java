package org.xyattic.eventual.consistency.support.core.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xyattic.eventual.consistency.support.core.rabbit.aop.RabbitConsumer;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.PendingMessageContextHolder;
import org.xyattic.eventual.consistency.support.core.rabbit.provider.aop.SendMqMessage;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Component
@RestController
public class TestListener {

    @RabbitListener(queues = "#{testQueue}")
    @RabbitConsumer
    public void rabbitListener(TestMessage testMessage) {
        System.out.println("========rabbitListener=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }

    @RabbitListener(queues = "#{testQueue2}")
    @RabbitConsumer
    public void rabbitListener2(TestMessage testMessage) {
        System.out.println("========rabbitListener2=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }

    @SendMqMessage
    @GetMapping("/guest/testSend2")
    public List<PendingMessage> testSend() throws Exception {
//        String id="f76c6ef9-9c4a-4b5e-a786-a947cf554092";
        String id = UUID.randomUUID().toString();
        TestMessage testMessage = new TestMessage(id, "ffs");

        PendingMessageContextHolder.set(Arrays.asList(
                PendingMessage.builder()
                        .appId("appid")
                        .body(testMessage)
                        .exchange("test-e")
                        .routingKey("test-k")
                        .messageId(id)
                        .build()
        ));
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
        return Arrays.asList(PendingMessage.builder()
                .appId("appid")
                .body(testMessage)
                .exchange("test-e")
                .routingKey("test-k")
                .messageId(id)
                .build());
    }
}