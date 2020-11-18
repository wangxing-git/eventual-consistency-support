package org.xyattic.eventual.consistency.support.core.autoconfigure

import com.google.common.collect.Maps
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.CollectionUtils
import org.xyattic.eventual.consistency.support.core.consumer.ConsumedMessage
import org.xyattic.eventual.consistency.support.core.persistence.Persistence
import org.xyattic.eventual.consistency.support.core.persistence.impl.JdbcTemplatePersistence
import org.xyattic.eventual.consistency.support.core.persistence.impl.MongoPersistence
import org.xyattic.eventual.consistency.support.core.provider.PendingMessage
import org.xyattic.eventual.consistency.support.core.provider.enums.PendingMessageStatus
import org.xyattic.eventual.consistency.support.core.utils.getLogger
import java.util.*
import java.util.function.Consumer

/**
 * @author wangxing
 * @create 2020/11/16
 */

@Configuration(proxyBeanMethods = false)
class DatabaseConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MongoTemplate::class)
    @ConditionalOnBean(MongoDatabaseFactory::class, MongoTemplate::class)
    @ConditionalOnProperty(name = ["eventual-consistency.database-type"], havingValue = "mongodb", matchIfMissing = true)
    internal class MongoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        fun mongoPersistence(mongoTemplate: MongoTemplate): Persistence {
            return MongoPersistence(mongoTemplate)
        }

        @Bean
        @ConditionalOnMissingBean
        fun mongoTransactionManager(mongoDatabaseFactory: MongoDatabaseFactory): PlatformTransactionManager {
            return MongoTransactionManager(mongoDatabaseFactory)
        }

        @Bean
        @ConditionalOnMissingBean
        fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
            return TransactionTemplate(transactionManager)
        }

    }

    @Configuration
    @ConditionalOnClass(JdbcTemplate::class)
    @ConditionalOnBean(JdbcTemplate::class)
    @ConditionalOnProperty(name = ["eventual-consistency.database-type"], havingValue = "jdbc", matchIfMissing = true)
    internal class JdbcConfiguration {

        private val log = getLogger()

        @Bean
        @ConditionalOnMissingBean
        fun jdbcTemplaPersistence(): Persistence {
            return JdbcTemplatePersistence()
        }

        @Bean
        @ConditionalOnMissingBean(name = ["initPersistenceDatabaseBean"])
        fun initPersistenceDatabaseBean(jdbcTemplate: JdbcTemplate): InitializingBean {
            return InitializingBean {
                var tableName = jdbcTemplate.queryForList("SELECT table_name FROM " +
                        "information_schema.TABLES WHERE table_name =?", String::class.java,
                        "consumed_message")
                if (CollectionUtils.isEmpty(tableName)) {
                    //创建表
                    try {
                        jdbcTemplate.execute("""
                            CREATE TABLE `consumed_message`  (
                              `id` char(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                              `message` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                              `success` int(1) NULL DEFAULT NULL,
                              `exception` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                              `consume_time` datetime(6) NULL DEFAULT NULL,
                              `create_time` datetime(6) NULL DEFAULT NULL,
                              PRIMARY KEY (`id`) USING BTREE
                            ) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
                        """.trimIndent())
                    } catch (e: Exception) {
                        log.warn("create table 'consumed_message' failed", e)
                    }
                }
                tableName = jdbcTemplate.queryForList("""
                        SELECT table_name FROM information_schema.TABLES WHERE table_name =?
                    """.trimIndent(), String::class.java, "pending_message")
                if (CollectionUtils.isEmpty(tableName)) {
                    //创建表
                    try {
                        jdbcTemplate.execute("""
                            CREATE TABLE `pending_message`  (
                              `message_id` char(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
                              `body` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                              `destination` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                              `headers` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
                              `status` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                              `persistence_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                              `transaction_manager` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
                              `send_time` datetime(6) NULL DEFAULT NULL,
                              `create_time` datetime(6) NULL DEFAULT NULL,
                              PRIMARY KEY (`message_id`) USING BTREE
                            ) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
                        """.trimIndent())
                    } catch (e: Exception) {
                        log.warn("create table 'pending_message' failed", e)
                    }
                }
            }
        }
    }

    @Configuration
    @ConditionalOnProperty(name = ["eventual-consistency.database-type"], havingValue = "in_memory", matchIfMissing = true)
    internal class InMemoryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        fun inMemoryPersistence(): Persistence {
            return InMemoryPersistence()
        }

        private class InMemoryPersistence : Persistence {

            private val pendingMessageMap = Maps.newConcurrentMap<String, PendingMessage>()
            private val consumedMessageMap = Maps.newConcurrentMap<Any, ConsumedMessage>()

            override fun save(consumedMessage: ConsumedMessage) {
                val old = consumedMessageMap.putIfAbsent(consumedMessage.id, consumedMessage)
                if (old != null) {
                    throw DuplicateKeyException(consumedMessage.id.toString())
                }
            }

            override fun save(pendingMessages: List<PendingMessage>) {
                pendingMessages.forEach(Consumer { pendingMessage: PendingMessage -> pendingMessageMap[pendingMessage.messageId] = pendingMessage })
            }

            override fun changePendingMessageStatus(id: String, status: PendingMessageStatus, sendTime: Date) {
                pendingMessageMap.computeIfPresent(id) { s: String, pendingMessage: PendingMessage ->
                    pendingMessage.status = status
                    pendingMessage.sendTime = sendTime
                    pendingMessage
                }
            }

            override fun getPendingMessages(timeBefore: Date): List<PendingMessage> {
                return pendingMessageMap.values
                        .filter { PendingMessageStatus.PENDING == it.status && it.createTime.before(timeBefore) }
            }
        }
    }

}