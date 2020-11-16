package org.xyattic.eventual.consistency.support.example.consumer.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author wangxing
 * @create 2020/4/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestMessage implements Serializable {

    private String eventId;

    private String name;

}