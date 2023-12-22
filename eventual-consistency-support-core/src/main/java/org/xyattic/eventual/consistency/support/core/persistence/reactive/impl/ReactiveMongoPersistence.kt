package org.xyattic.eventual.consistency.support.core.persistence.reactive.impl

import org.springframework.beans.factory.DisposableBean
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.reactive.ReactivePersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.PendingMessageHeaders
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.*

/**
 * @author wangxing
 * @create 2020/4/14
 */
open class ReactiveMongoPersistence : ReactivePersistence, DisposableBean {

    companion object {
        private val log = getLogger()
    }

    private var mongoTemplate: ReactiveMongoTemplate

    private val scheduler: Scheduler

    constructor(mongoTemplate: ReactiveMongoTemplate) {
        this.mongoTemplate = mongoTemplate
        scheduler = Schedulers.newBoundedElastic(10, Int.MAX_VALUE, "ReactiveMongoPersistence")
    }

    constructor(mongoTemplate: ReactiveMongoTemplate, scheduler: Scheduler) {
        this.mongoTemplate = mongoTemplate
        this.scheduler = scheduler
    }

    override fun save(consumedMessage: ConsumedMessage): Mono<Void> {
        return mongoTemplate.exists(Query.query(Criteria.where("_id").`is`(consumedMessage.id)), ConsumedMessage::class.java)
                .flatMap {
                    if (it) {
                        Mono.error(DuplicateKeyException(consumedMessage.id.toString()))
                    } else {
                        mongoTemplate.insert(consumedMessage).then()
                    }
                }
    }

    override fun save(pendingMessage: PendingMessage): Mono<Void> {
        return mongoTemplate.exists(Query.query(Criteria.where("_id").`is`(pendingMessage.messageId)), PendingMessage::class.java)
                .flatMap {
                    if (!it) {
                        mongoTemplate.insert(pendingMessage)
                                .then()
                    } else {
                        Mono.error(DuplicateKeyException(pendingMessage.messageId))
                    }
                }
    }

    override fun changePendingMessageStatus(
            id: String,
            status: PendingMessageStatus,
            sendTime: Date
    ): Mono<Void> {
        val query = Query.query(Criteria.where("_id").`is`(id))
        val update = Update.update("status", status)
                .set("sendTime", sendTime)
        return mongoTemplate.update(PendingMessage::class.java)
                .matching(query)
                .apply(update)
                .first()
                .then()
    }

    override fun sendSuccess(id: String, messageId: String): Mono<Void> {
        val query = Query.query(Criteria.where("_id").`is`(id))
        val update = Update.update("status", PendingMessageStatus.HAS_BEEN_SENT)
                .set("sendTime", Date())
                .set("headers." + PendingMessageHeaders.msgIdHeader, messageId)
        return mongoTemplate.update(PendingMessage::class.java)
                .matching(query)
                .apply(update)
                .first()
                .then()
    }

    override fun sendFailed(id: String): Mono<Void> {
        val query = Query.query(Criteria.where("_id").`is`(id))
        val update = Update.update("status", PendingMessageStatus.FAILED_TO_SEND)
        return mongoTemplate.update(PendingMessage::class.java)
                .matching(query)
                .apply(update)
                .first()
                .then()
    }

    override fun getPendingMessages(timeBefore: Date): Flux<PendingMessage> {
        return mongoTemplate.find(
                Query.query(
                        Criteria.where("status").`is`(PendingMessageStatus.PENDING).and("createTime")
                                .lte(timeBefore)
                ), PendingMessage::class.java
        )
    }

    override fun destroy() {
    }

}