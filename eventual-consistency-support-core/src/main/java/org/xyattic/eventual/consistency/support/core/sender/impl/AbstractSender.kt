package org.xyattic.eventual.consistency.support.core.sender.impl

import org.springframework.transaction.support.TransactionTemplate
import org.xyattic.eventual.consistency.support.core.persistence.ProviderPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import org.xyattic.eventual.consistency.support.core.sender.Sender
import org.xyattic.eventual.consistency.support.core.utils.SpringBeanUtils
import org.xyattic.eventual.consistency.support.core.utils.getLogger

/**
 * @author wangxing
 * @create 2020/11/17
 */
abstract class AbstractSender:Sender {

    companion object {
        private val log = getLogger()
    }

    fun changePendingMessageStatus(id: String, status: PendingMessageStatus){

    }

    fun getTransactionTemplate(pendingMessage: PendingMessage): TransactionTemplate {
        return SpringBeanUtils.getBean(pendingMessage.transactionManager, TransactionTemplate::class.java)
    }

    fun getTransactionTemplate(name: String?): TransactionTemplate {
        return SpringBeanUtils.getBean(name, TransactionTemplate::class.java)
    }

    fun getProviderPersistence(name: String?): ProviderPersistence {
        return SpringBeanUtils.getBean(name, ProviderPersistence::class.java)
    }

}