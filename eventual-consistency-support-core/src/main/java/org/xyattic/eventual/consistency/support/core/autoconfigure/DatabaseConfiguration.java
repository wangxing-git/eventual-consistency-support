package org.xyattic.eventual.consistency.support.core.autoconfigure;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage;
import org.xyattic.eventual.consistency.support.core.persistence.Persistence;
import org.xyattic.eventual.consistency.support.core.persistence.impl.JdbcTemplatePersistence;
import org.xyattic.eventual.consistency.support.core.persistence.impl.MongoPersistence;
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage;
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author wangxing
 * @create 2020/4/8
 */
@Configuration
public class DatabaseConfiguration {

    @Configuration
    @ConditionalOnClass(MongoTemplate.class)
    @ConditionalOnBean({MongoDbFactory.class, MongoTemplate.class})
    @ConditionalOnProperty(name = "eventual-consistency.database-type", havingValue = "mongodb",
            matchIfMissing = true)
    static class MongoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Persistence mongoPersistence(MongoTemplate mongoTemplate) {
            return new MongoPersistence(mongoTemplate);
        }

        @Bean
        @ConditionalOnMissingBean
        public PlatformTransactionManager mongoTransactionManager(MongoDbFactory mongoDbFactory) {
            return new MongoTransactionManager(mongoDbFactory);
        }

        @Bean
        @ConditionalOnMissingBean
        public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

    }

    @Slf4j
    @Configuration
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnProperty(name = "eventual-consistency.database-type", havingValue = "jdbc",
            matchIfMissing = true)
    static class JdbcConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Persistence jdbcTemplaPersistence() {
            return new JdbcTemplatePersistence();
        }

        @Bean
        @ConditionalOnMissingBean(name = "initPersistenceDatabaseBean")
        public InitializingBean initPersistenceDatabaseBean(JdbcTemplate jdbcTemplate) {
            return () -> {
                List<String> tableName = jdbcTemplate.queryForList("SELECT table_name FROM " +
                                "information_schema.TABLES WHERE table_name =?", String.class,
                        "consumed_message");
                if (CollectionUtils.isEmpty(tableName)) {
                    //创建表
                    try {
                        jdbcTemplate.execute("CREATE TABLE `consumed_message` (`id` CHAR (64) " +
                                "CHARACTER\n" +
                                "SET utf8 COLLATE utf8_general_ci NOT NULL,`message` LONGTEXT " +
                                "CHARACTER\n" +
                                "SET utf8 COLLATE utf8_general_ci NULL,`success` INT (1) NULL " +
                                "DEFAULT" +
                                " NULL,`exception` LONGTEXT CHARACTER\n" +
                                "SET utf8 COLLATE utf8_general_ci NULL,`create_time` datetime (6)" +
                                " " +
                                "NULL DEFAULT NULL,PRIMARY KEY (`id`) USING BTREE) ENGINE=INNODB " +
                                "CHARACTER\n" +
                                "SET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=Dynamic;");
                    } catch (Exception e) {
                        log.warn("create table 'consumed_message' failed", e);
                    }
                }

                tableName = jdbcTemplate.queryForList("SELECT table_name FROM " +
                                "information_schema.TABLES WHERE table_name =?", String.class,
                        "pending_message");
                if (CollectionUtils.isEmpty(tableName)) {
                    //创建表
                    try {
                        jdbcTemplate.execute("CREATE TABLE `pending_message`  (\n" +
                                "  `message_id` char(64) CHARACTER SET utf8 COLLATE " +
                                "utf8_general_ci NOT NULL,\n" +
                                "  `body` longtext CHARACTER SET utf8 COLLATE utf8_general_ci " +
                                "NULL,\n" +
                                "  `destination` varchar(255) CHARACTER SET utf8 COLLATE " +
                                "utf8_general_ci NULL DEFAULT NULL,\n" +
                                "  `headers` longtext CHARACTER SET utf8 COLLATE utf8_general_ci " +
                                "NULL,\n" +
                                "  `status` varchar(255) CHARACTER SET utf8 COLLATE " +
                                "utf8_general_ci NULL DEFAULT NULL,\n" +
                                "  `persistence_name` varchar(255) CHARACTER SET utf8 COLLATE " +
                                "utf8_general_ci NULL DEFAULT NULL,\n" +
                                "  `transaction_manager` varchar(255) CHARACTER SET utf8 COLLATE " +
                                "utf8_general_ci NULL DEFAULT NULL,\n" +
                                "  `create_time` datetime(6) NULL DEFAULT NULL,\n" +
                                "  PRIMARY KEY (`message_id`) USING BTREE\n" +
                                ") ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci" +
                                " ROW_FORMAT = Dynamic;");
                    } catch (Exception e) {
                        log.warn("create table 'pending_message' failed", e);
                    }
                }
            };
        }

    }

    @Configuration
    @ConditionalOnProperty(name = "eventual-consistency.database-type", havingValue = "in_memory"
            , matchIfMissing = true)
    static class InMemoryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Persistence inMemoryPersistence() {
            return new InMemoryPersistence();
        }

        static class InMemoryPersistence implements Persistence {

            private final ConcurrentMap<String, PendingMessage> pendingMessageMap =
                    Maps.newConcurrentMap();
            private final ConcurrentMap<Object, ConsumedMessage> consumedMessageMap =
                    Maps.newConcurrentMap();

            @Override
            public void save(ConsumedMessage consumedMessage) {
                final ConsumedMessage old =
                        consumedMessageMap.putIfAbsent(consumedMessage.getId(), consumedMessage);
                if (old != null) {
                    throw new DuplicateKeyException(consumedMessage.getId().toString());
                }
            }

            @Override
            public void save(List<PendingMessage> pendingMessages) {
                pendingMessages.forEach(pendingMessage -> pendingMessageMap.put(pendingMessage.getMessageId(), pendingMessage));
            }

            @Override
            public void changePendingMessageStatus(String id, PendingMessageStatus status) {
                pendingMessageMap.computeIfPresent(id, (s, pendingMessage) -> {
                    pendingMessage.setStatus(status);
                    return pendingMessage;
                });
            }

            @Override
            public List<PendingMessage> getPendingMessages(Date timeBefore) {
                return pendingMessageMap.values()
                        .stream()
                        .filter(pendingMessage -> PendingMessageStatus.PENDING.equals(pendingMessage.getStatus()))
                        .filter(pendingMessage -> pendingMessage.getCreateTime().before(timeBefore))
                        .collect(Collectors.toList());
            }
        }

    }

}