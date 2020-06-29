package org.xyattic.eventual.consistency.support.core.utils;

import com.google.common.collect.Lists;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.LinkedList;

public class XyatticSpringApplicationRunListener implements SpringApplicationRunListener {

    private static final LinkedList<ConfigurableApplicationContext> APPLICATION_CONTEXTS = Lists.newLinkedList();

    public XyatticSpringApplicationRunListener(final SpringApplication application, final String[] args) {
    }

    @Override
    public void starting() {
    }

    @Override
    public void environmentPrepared(final ConfigurableEnvironment environment) {
    }

    @Override
    public void contextPrepared(final ConfigurableApplicationContext context) {
    }

    @Override
    public void contextLoaded(final ConfigurableApplicationContext context) {
        APPLICATION_CONTEXTS.add(context);
    }

    @Override
    public void started(final ConfigurableApplicationContext context) {
    }

    @Override
    public void running(final ConfigurableApplicationContext context) {
    }

    @Override
    public void failed(final ConfigurableApplicationContext context, final Throwable exception) {
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        if (APPLICATION_CONTEXTS.isEmpty()) {
            return null;
        }
        ConfigurableApplicationContext applicationContext = APPLICATION_CONTEXTS.getLast();
        try {
            applicationContext.getAutowireCapableBeanFactory();
        } catch (IllegalStateException e) {
            APPLICATION_CONTEXTS.removeLast();
            return getApplicationContext();
        }
        return applicationContext;
    }

}