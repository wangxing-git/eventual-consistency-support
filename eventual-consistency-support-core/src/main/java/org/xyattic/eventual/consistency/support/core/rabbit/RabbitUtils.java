package org.xyattic.eventual.consistency.support.core.rabbit;

import lombok.SneakyThrows;
import org.xyattic.boot.calkin.core.utils.SpringContextUtils;
import org.xyattic.boot.calkin.core.utils.WebUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author wangxing
 * @create 2020/3/28
 */
public class RabbitUtils {

    @SneakyThrows
    private static String get(String str) {
        List<String> activeProfiles =
                Arrays.asList(SpringContextUtils.getEnvironment().getActiveProfiles());
        return activeProfiles.contains("local") ||
                activeProfiles.contains("dev") ? WebUtils.getServerIpAddress().getHostAddress() + "_" + str : str;
    }

    public static String getQueue(String queue) {
        return get(queue);
    }

    public static String getRoutingKey(String routingKey) {
        return get(routingKey);
    }

    public static String getExchange(String exchange) {
        return get(exchange);
    }

}