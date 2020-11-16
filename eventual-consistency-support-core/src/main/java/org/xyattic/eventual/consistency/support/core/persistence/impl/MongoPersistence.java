package org.xyattic.eventual.consistency.support.core.persistence.impl;

import lombok.NonNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage;
import org.xyattic.eventual.consistency.support.core.persistence.Persistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * @author wangxing
 * @create 2020/4/14
 */
public class MongoPersistence implements Persistence {

    private MongoTemplate mongoTemplate;

    private ForkJoinPool forkJoinPool;

    public MongoPersistence(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.forkJoinPool = new ForkJoinPool(50);
    }

    public MongoPersistence(MongoTemplate mongoTemplate, ForkJoinPool forkJoinPool) {
        this.mongoTemplate = mongoTemplate;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    public void save(ConsumedMessage consumedMessage) {
        createCollection(ConsumedMessage.class);
        mongoTemplate.insert(consumedMessage);
    }

    protected void createCollection(Class<?> clz) {
        forkJoinPool.submit(() -> {
            if (!mongoTemplate.collectionExists(clz)) {
                try {
                    mongoTemplate.createCollection(clz);
                } catch (Exception ignored) {
                }
            }
        }).join();
    }

    @Override
    public void save(List<PendingMessage> pendingMessages) {
        createCollection(PendingMessage.class);
        mongoTemplate.insertAll(pendingMessages);
    }

    @Override
    public void changePendingMessageStatus(@NonNull String id, PendingMessageStatus status) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = Update.update("status", status);
        mongoTemplate.update(PendingMessage.class).matching(query).apply(update).all();
    }

    @Override
    public List<PendingMessage> getPendingMessages(Date timeBefore) {
        return mongoTemplate.find(Query.query(Criteria.where("status").is(PendingMessageStatus.PENDING).and("createTime").lte(timeBefore)), PendingMessage.class);
    }

}