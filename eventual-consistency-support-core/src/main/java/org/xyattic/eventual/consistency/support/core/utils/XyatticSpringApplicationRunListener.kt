package org.xyattic.eventual.consistency.support.core.utils

import com.google.common.collect.Lists
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

open class XyatticSpringApplicationRunListener(application: SpringApplication?, args: Array<String?>?) : SpringApplicationRunListener {
    override fun starting() {}
    override fun environmentPrepared(environment: ConfigurableEnvironment) {}
    override fun contextPrepared(context: ConfigurableApplicationContext) {}
    override fun contextLoaded(context: ConfigurableApplicationContext) {
        APPLICATION_CONTEXTS.add(context)
    }

    override fun started(context: ConfigurableApplicationContext) {}
    override fun running(context: ConfigurableApplicationContext) {}
    override fun failed(context: ConfigurableApplicationContext, exception: Throwable) {}

    companion object {
        private val APPLICATION_CONTEXTS = Lists.newLinkedList<ConfigurableApplicationContext>()
        val applicationContext: ConfigurableApplicationContext?
            get() {
                if (APPLICATION_CONTEXTS.isEmpty()) {
                    return null
                }
                val applicationContext = APPLICATION_CONTEXTS.last
                try {
                    applicationContext.autowireCapableBeanFactory
                } catch (e: IllegalStateException) {
                    APPLICATION_CONTEXTS.removeLast()
                    return Companion.applicationContext
                }
                return applicationContext
            }
    }
}