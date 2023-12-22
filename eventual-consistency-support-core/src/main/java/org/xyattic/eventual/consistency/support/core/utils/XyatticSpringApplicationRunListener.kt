package org.xyattic.eventual.consistency.support.core.utils

import com.google.common.collect.Lists
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import java.util.concurrent.locks.ReentrantLock

open class XyatticSpringApplicationRunListener(
    application: SpringApplication?,
    args: Array<String?>?
) : SpringApplicationRunListener {
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
        private val APPLICATION_CONTEXTS =
            Lists.newCopyOnWriteArrayList<ConfigurableApplicationContext>()

        private val CHILD_APPLICATION_CONTEXTS =
            Lists.newCopyOnWriteArrayList<ConfigurableApplicationContext>()

        private val lock = ReentrantLock()

        val applicationContext: ConfigurableApplicationContext?
            get() {
                if (APPLICATION_CONTEXTS.isEmpty()) {
                    return null
                }
                lock.lock()
                try {
                    val applicationContext = APPLICATION_CONTEXTS.last()
                    try {
                        applicationContext.autowireCapableBeanFactory
                        if ("bootstrap".equals(
                                applicationContext.id,
                                ignoreCase = true
                            ) ||
                            (applicationContext.parent != null && !"bootstrap".equals(
                                applicationContext.parent.id,
                                ignoreCase = true
                            ))
                        ) {
                            APPLICATION_CONTEXTS.remove(applicationContext)
                            CHILD_APPLICATION_CONTEXTS.add(applicationContext)
                            return Companion.applicationContext
                        }
                    } catch (e: IllegalStateException) {
                        APPLICATION_CONTEXTS.remove(applicationContext)
                        return Companion.applicationContext
                    }
                    return applicationContext
                } finally {
                    lock.unlock()
                }
            }
    }
}