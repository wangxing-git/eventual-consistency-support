package org.xyattic.eventual.consistency.support.core.sender.impl

import org.springframework.transaction.reactive.TransactionalOperator
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactiveProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.sender.ReactiveSender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils

/**
 * @author wangxing
 * @create 2020/11/17
 */
abstract class AbstractReactiveSender:ReactiveSender {

    fun getTransactionalOperator(pendingMessage: PendingMessage): TransactionalOperator {
        return SpringBeanUtils.getBean(pendingMessage.transactionManager, TransactionalOperator::class.java)
    }

    fun getTransactionalOperator(name: String?): TransactionalOperator {
        return SpringBeanUtils.getBean(name, TransactionalOperator::class.java)
    }

    fun getReactiveProviderPersistence(name: String?): ReactiveProviderPersistence {
        return SpringBeanUtils.getBean(name, ReactiveProviderPersistence::class.java)
    }

}