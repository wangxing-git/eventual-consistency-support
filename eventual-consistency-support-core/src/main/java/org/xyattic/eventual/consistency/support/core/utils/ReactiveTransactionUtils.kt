package org.xyattic.eventual.consistency.support.core.utils

import com.google.common.collect.Maps
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils.getBean

/**
 * @author wangxing
 * @create 2021/9/7
 */
object ReactiveTransactionUtils {
    private val TRANSACTIONAL_OPERATOR_MAP: MutableMap<ReactiveTransactionManager, MutableMap<Int, TransactionalOperator>> =
        Maps.newConcurrentMap()

    /**
     * @param propagationBehavior @see [TransactionDefinition]
     * @return
     */
    fun getTransactionalOperator(propagationBehavior: Int): TransactionalOperator {
        return getTransactionalOperator(propagationBehavior, reactiveTransactionManager)
    }

    /**
     * @param propagationBehavior @see [TransactionDefinition]
     * @return
     */
    fun getTransactionalOperator(
        propagationBehavior: Int,
        reactiveTransactionManager: ReactiveTransactionManager
    ): TransactionalOperator {
        val transactionalOperatorMap: Map<Int, TransactionalOperator> =
            getTransactionalOperatorMap(reactiveTransactionManager)
        return transactionalOperatorMap[propagationBehavior]
            ?: return initOperator(propagationBehavior, reactiveTransactionManager)
    }

    private fun getTransactionalOperatorMap(reactiveTransactionManager: ReactiveTransactionManager): MutableMap<Int, TransactionalOperator> {
        var transactionalOperatorMap = TRANSACTIONAL_OPERATOR_MAP[reactiveTransactionManager]
        if (transactionalOperatorMap == null) {
            TRANSACTIONAL_OPERATOR_MAP.putIfAbsent(
                reactiveTransactionManager,
                Maps.newConcurrentMap()
            )
            transactionalOperatorMap = TRANSACTIONAL_OPERATOR_MAP[reactiveTransactionManager]
        }
        return transactionalOperatorMap!!
    }

    private fun initOperator(
        propagationBehavior: Int,
        reactiveTransactionManager: ReactiveTransactionManager
    ): TransactionalOperator {
        val transactionalOperatorMap = getTransactionalOperatorMap(reactiveTransactionManager)
        val transactionDefinition = DefaultTransactionDefinition()
        transactionDefinition.propagationBehavior = propagationBehavior
        val transactionalOperator =
            TransactionalOperator.create(reactiveTransactionManager, transactionDefinition)
        transactionalOperatorMap.putIfAbsent(propagationBehavior, transactionalOperator)
        return transactionalOperatorMap[propagationBehavior]!!
    }

    val reactiveTransactionManager: ReactiveTransactionManager
        get() = getBean(
            ReactiveTransactionManager::class.java
        )
    val transactionalOperator: TransactionalOperator
        get() = getTransactionalOperator(reactiveTransactionManager)

    fun getTransactionalOperator(reactiveTransactionManager: ReactiveTransactionManager): TransactionalOperator {
        return getTransactionalOperator(
            TransactionDefinition.PROPAGATION_REQUIRED,
            reactiveTransactionManager
        )
    }

    val requestsNewOperator: TransactionalOperator
        get() = getRequestsNewOperator(reactiveTransactionManager)

    fun getRequestsNewOperator(reactiveTransactionManager: ReactiveTransactionManager): TransactionalOperator {
        return getTransactionalOperator(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            reactiveTransactionManager
        )
    }
}