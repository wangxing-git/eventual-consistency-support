package org.xyattic.eventual.consistency.support.core.utils

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Environment

/**
 * @author wangxing
 * @create 2020/4/8
 */
object SpringContextUtils : ApplicationContextAware {
    private var applicationContextValue: ConfigurableApplicationContext? = null
    val applicationContext: ConfigurableApplicationContext?
        get() {
            if (applicationContextValue == null) {
                return XyatticSpringApplicationRunListener.applicationContext
            }
            return applicationContextValue;
        }
    val environment: Environment
        get() = applicationContext!!.environment

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        applicationContextValue = applicationContext.safeAs();
    }

}
