package org.xyattic.eventual.consistency.support.core.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author wangxing
 * @create 2020/3/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class commonMessage implements Serializable {
    private String content;
}