package org.xyattic.eventual.consistency.support.core.utils

import com.google.common.collect.Maps
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils.getBean

/**
 * @author wangxing
 * @create 2021/9/7
 */
object TransactionUtils {
    private val TRANSACTIONAL_TEMPLETE_MAP: MutableMap<PlatformTransactionManager, MutableMap<Int, TransactionTemplate>> =
        Maps.newConcurrentMap()

    /**
     * @param propagationBehavior @see [TransactionDefinition]
     * @return
     */
    fun getTransactionTemplate(propagationBehavior: Int): TransactionTemplate {
        return getTransactionTemplate(propagationBehavior, platformTransactionManager)
    }

    /**
     * @param propagationBehavior @see [TransactionDefinition]
     * @return
     */
    fun getTransactionTemplate(
        propagationBehavior: Int,
        platformTransactionManager: PlatformTransactionManager
    ): TransactionTemplate {
        val transactionTemplateMap: Map<Int, TransactionTemplate> =
            getTransactionTemplateMap(platformTransactionManager)
        return transactionTemplateMap[propagationBehavior]
            ?: return initTemplate(propagationBehavior, platformTransactionManager)
    }

    private fun getTransactionTemplateMap(transactionManager: PlatformTransactionManager): MutableMap<Int, TransactionTemplate> {
        var transactionalOperatorMap = TRANSACTIONAL_TEMPLETE_MAP[transactionManager]
        if (transactionalOperatorMap == null) {
            TRANSACTIONAL_TEMPLETE_MAP.putIfAbsent(
                transactionManager,
                Maps.newConcurrentMap()
            )
            transactionalOperatorMap = TRANSACTIONAL_TEMPLETE_MAP[transactionManager]
        }
        return transactionalOperatorMap!!
    }

    private fun initTemplate(
        propagationBehavior: Int,
        transactionManager: PlatformTransactionManager
    ): TransactionTemplate {
        val transactionTemplateMap = getTransactionTemplateMap(transactionManager)
        val transactionDefinition = DefaultTransactionDefinition()
        transactionDefinition.propagationBehavior = propagationBehavior
        val transactionTemplate = TransactionTemplate(transactionManager, transactionDefinition)
        transactionTemplateMap.putIfAbsent(propagationBehavior, transactionTemplate)
        return transactionTemplateMap[propagationBehavior]!!
    }

    val platformTransactionManager: PlatformTransactionManager
        get() = getBean(
            PlatformTransactionManager::class.java
        )
    val transactionTemplate: TransactionTemplate
        get() = getTransactionTemplate(platformTransactionManager)

    fun getTransactionTemplate(platformTransactionManager: PlatformTransactionManager): TransactionTemplate {
        return getTransactionTemplate(
            TransactionDefinition.PROPAGATION_REQUIRED,
            platformTransactionManager
        )
    }

    val requestsNewTemplate: TransactionTemplate
        get() = getRequestsNewTemplate(platformTransactionManager)

    fun getRequestsNewTemplate(platformTransactionManager: PlatformTransactionManager): TransactionTemplate {
        return getTransactionTemplate(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            platformTransactionManager
        )
    }
}