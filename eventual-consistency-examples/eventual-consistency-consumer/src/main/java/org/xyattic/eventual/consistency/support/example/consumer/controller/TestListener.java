package org.xyattic.eventual.consistency.support.example.consumer.controller;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Component
@RestController
public class TestListener {

    @RabbitListener(queues = "test-q10")
    @ConsumeTestMessage
    public void rabbitListener(TestMessage testMessage) {
        System.out.println("========rabbitListener=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }

    @RabbitListener(queues = "test-q20")
    @ConsumeTestMessage
    public void rabbitListener2(TestMessage testMessage) {
        System.out.println("========rabbitListener2=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }

}