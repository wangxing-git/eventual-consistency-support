package org.xyattic.eventual.consistency.support.core.utils

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Environment

/**
 * @author wangxing
 * @create 2020/4/8
 */
object SpringContextUtils {
    val applicationContext: ConfigurableApplicationContext?
        get() = XyatticSpringApplicationRunListener.applicationContext
    val environment: Environment
        get() = applicationContext!!.environment
}