package org.xyattic.eventual.consistency.support.core.rabbit.provider;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author wangxing
 * @create 2020/4/1
 */
public class PendingMessageContextHolder {

    private static final ThreadLocal<List<PendingMessage>> contextHolder =
            ThreadLocal.withInitial(Lists::newArrayList);

    public static void set(List<PendingMessage> pendingMessages) {
        contextHolder.set(pendingMessages);
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static List<PendingMessage> get() {
        return contextHolder.get();
    }

    public static void addAll(List<PendingMessage> pendingMessages) {
        contextHolder.get().addAll(pendingMessages);
    }

    public static void add(PendingMessage pendingMessage) {
        contextHolder.get().add(pendingMessage);
    }

}