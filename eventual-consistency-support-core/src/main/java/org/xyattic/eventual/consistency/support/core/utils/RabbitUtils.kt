package org.xyattic.eventual.consistency.support.core.utils

/**
 * @author wangxing
 * @create 2020/3/28
 */
class RabbitUtils {

    companion object {

        @JvmStatic
        fun getQueue(queue: String): String {
            return RabbitUtils.get(queue)
        }

        @JvmStatic
        fun getRoutingKey(routingKey: String): String {
            return RabbitUtils.get(routingKey)
        }

        @JvmStatic
        fun getExchange(exchange: String): String {
            return RabbitUtils.get(exchange)
        }

        private fun get(str: String): String {
//        List<String> activeProfiles =
//                Arrays.asList(SpringContextUtils.getEnvironment().getActiveProfiles());
//        return activeProfiles.contains("local") ||
//                activeProfiles.contains("dev") ? WebUtils.getServerIpAddress().getHostAddress() + "_" + str : str;
            return str
        }
    }


}