package org.xyattic.eventual.consistency.support.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wangxing
 * @create 2020/11/12
 */
@Data
@ConfigurationProperties("eventual-consistency")
public class EventualConsistency {

    private Boolean enabled = true;

    private DatabaseType databaseType;

    public enum DatabaseType {
        mongodb, jdbc, In_Memory;
    }
}