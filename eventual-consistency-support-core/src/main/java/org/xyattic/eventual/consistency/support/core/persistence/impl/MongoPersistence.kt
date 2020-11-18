package org.xyattic.eventual.consistency.support.core.persistence.impl

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.Persistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import java.util.*
import java.util.concurrent.ForkJoinPool

/**
 * @author wangxing
 * @create 2020/4/14
 */
open class MongoPersistence : Persistence {
    private var mongoTemplate: MongoTemplate
    private var forkJoinPool: ForkJoinPool

    constructor(mongoTemplate: MongoTemplate) {
        this.mongoTemplate = mongoTemplate
        forkJoinPool = ForkJoinPool(10)
    }

    constructor(mongoTemplate: MongoTemplate, forkJoinPool: ForkJoinPool) {
        this.mongoTemplate = mongoTemplate
        this.forkJoinPool = forkJoinPool
    }

    override fun save(consumedMessage: ConsumedMessage) {
        createCollection(ConsumedMessage::class.java)
        mongoTemplate.insert(consumedMessage)
    }

    protected fun createCollection(clz: Class<*>) {
        forkJoinPool.submit {
            if (!mongoTemplate.collectionExists(clz)) {
                try {
                    mongoTemplate.createCollection(clz)
                } catch (ignored: Exception) {
                }
            }
        }.join()
    }

    override fun save(pendingMessages: List<PendingMessage>) {
        createCollection(PendingMessage::class.java)
        mongoTemplate.insertAll(pendingMessages)
    }

    override fun changePendingMessageStatus(id: String, status: PendingMessageStatus, sendTime: Date) {
        val query = Query.query(Criteria.where("_id").`is`(id))
        val update = Update.update("status", status)
                .set("sendTime", sendTime)
        mongoTemplate.update(PendingMessage::class.java).matching(query).apply(update).all()
    }

    override fun getPendingMessages(timeBefore: Date): List<PendingMessage> {
        return mongoTemplate.find(Query.query(Criteria.where("status").`is`(PendingMessageStatus.PENDING).and("createTime").lte(timeBefore)), PendingMessage::class.java)
    }
}