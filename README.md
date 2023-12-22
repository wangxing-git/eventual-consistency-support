## 如何使用

### 生产者

1. 引入pom依赖

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-amqp</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-mongodb</artifactId>
   </dependency>
   <dependency>
       <groupId>org.xyattic</groupId>
       <artifactId>eventual-consistency-support-core</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </dependency>
   ```

2. 开启`@EnableReSendJob`

   ```java
   @Configuration
   @EnableRabbit
   @EnableReSendJob
   public class RabbitConfig {
   }
   ```

3. 配置rabbit和mongo

   ```properties
   spring.data.mongodb.uri=mongodb://root:123456@localhost:27017/marketing?replicaSet=rs0&readPreference=primary&authSource=admin
   
   #rabbitMQ
   spring.rabbitmq.host=localhost
   spring.rabbitmq.port=5672
   spring.rabbitmq.username=user
   spring.rabbitmq.password=password
   spring.rabbitmq.publisher-confirm-type=correlated
   spring.rabbitmq.publisher-returns=true
   spring.rabbitmq.template.mandatory=true
   spring.rabbitmq.listener.simple.acknowledge-mode=auto
   ```

   

4. 声明`Exchange` `Queue` `Binding`

   ```java
   @Bean
   public Binding testBinding() {
       return BindingBuilder.bind(testQueue()).to(testExchange()).with("test-k");
   }
   
   @Bean
   public DirectExchange testExchange() {
       return new DirectExchange("test-e");
   }
   
   @Bean
   @SneakyThrows
   public Queue testQueue() {
       return new Queue("test-q10");
   }
   ```

   

5. 在需要发送的方法上进行标注`@SendMqMessage`

   > 注: 只有spring管理的对象才能生效

```java
    @SendMqMessage
    @GetMapping("/guest/testSend")
    public List<PendingMessage> testSend() throws Exception {
        String id = UUID.randomUUID().toString();
        TestMessage testMessage = new TestMessage(id, "ffs");

        //方式1: 覆盖设置
        PendingMessageContextHolder.set(Arrays.asList(
                PendingMessage.builder()
                    .setHeader(RabbitConstants.APP_ID_HEADER, "appid")
                    .body(testMessage)
                    .setHeader(RabbitConstants.EXCHANGE_HEADER, "test-e")
                    .destination("test-k")
                    .messageId(id)
                    .build()
        ));
        //方式2: 此方法是添加,不是覆盖
//        PendingMessageContextHolder.addAll(Arrays.asList(
//                PendingMessage.builder()
//                      .setHeader(RabbitConstants.APP_ID_HEADER, "appid")
//                      .body(testMessage)
//                      .setHeader(RabbitConstants.EXCHANGE_HEADER, "test-e")
//                      .destination("test-k")
//                      .messageId(id)
//                      .build()
//        ));
        //方式3: 此方法是添加,不是覆盖
//        PendingMessageContextHolder.add(
//                PendingMessage.builder()
//                      .setHeader(RabbitConstants.APP_ID_HEADER, "appid")
//                      .body(testMessage)
//                      .setHeader(RabbitConstants.EXCHANGE_HEADER, "test-e")
//                      .destination("test-k")
//                      .messageId(id)
//                      .build()
//        );
        //方式4: 覆盖设置
        return Arrays.asList(PendingMessage.builder()
                     .setHeader(RabbitConstants.APP_ID_HEADER, "appid")
                     .body(testMessage)
                     .setHeader(RabbitConstants.EXCHANGE_HEADER, "test-e")
                     .destination("test-k")
                     .messageId(id)
                     .build());
        //方式5: 覆盖设置
//      return PendingMessage.builder()
//                .setHeader(RabbitConstants.APP_ID_HEADER, "appid")
//                .body(testMessage)
//                .setHeader(RabbitConstants.EXCHANGE_HEADER, "test-e")
//                .destination("test-k")
//                .messageId(id)
//                .build();
    }
```

> 注: 方式4和方式5具有更高的优先级,上例方式4将覆盖方式1,2,3的设置
>
> 使用时需要用spring注入的代理对象进行调用,不可直接使用`this.xxx`调用

```
2020-04-09 11:13:02.925  INFO 32020 --- [nio-8088-exec-1] c.c.n.c.core.provider.RabbitSender       : message sent successfully: PendingMessage(messageId=6888a20a-2b43-4ac7-9233-6d564f888b43, appId=appid, body=TestMessage(eventId=6888a20a-2b43-4ac7-9233-6d564f888b43, name=ffs), exchange=test-e, routingKey=test-k2, status=PENDING, mongoTemplate=, transactionManager=, createTime=Thu Apr 09 11:13:01 CST 2020)

```

### 消费者

1. 引入pom依赖

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-amqp</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-mongodb</artifactId>
   </dependency>
   <dependency>
       <groupId>org.xyattic</groupId>
       <artifactId>eventual-consistency-support-core</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </dependency>
   ```

2. 配置rabbit和mongo

   ```properties
   spring.data.mongodb.uri=mongodb://root:123456@localhost:27017/marketing?replicaSet=rs0&readPreference=primary&authSource=admin
   
   #rabbitMQ
   spring.rabbitmq.host=localhost
   spring.rabbitmq.port=5672
   spring.rabbitmq.username=user
   spring.rabbitmq.password=password
   spring.rabbitmq.publisher-confirm-type=correlated
   spring.rabbitmq.publisher-returns=true
   spring.rabbitmq.template.mandatory=true
   spring.rabbitmq.listener.simple.acknowledge-mode=auto
   ```

3. 使用`@RabbitListener`进行监听,并标注`@ConsumeMqMessage`

```java
    @RabbitListener(queues = "test-q10")
    @ConsumeMqMessage(messageClass = TestMessage.class, messageIdExpression = "#{message.eventId}")
    public void rabbitListener(TestMessage testMessage) {
        System.out.println("========rabbitListener=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }

    @RabbitListener(queues = "test-q20")
    @ConsumeMqMessage(messageClass = TestMessage.class, messageIdExpression = "#{message.eventId}")
    public void rabbitListener2(TestMessage testMessage) {
        System.out.println("========rabbitListener2=========");
        System.out.println(testMessage);
//        throw new RuntimeException("test");
    }
```

> queue `test-q10`和`test-q20`需已经存在,若不存在请自行创建和绑定

### 多mongo库环境下的设置

如果当前应用中存在多个`mongoTemplate`对象并都需要进行相关发送和消费的话,需要设置对应的`MongoTemplate`和`TransactionTemplate`

如存在order库和user库两个都需要发送和消费:

```java
@Bean
public MongoPersistence orderMongoPersistence(){
	return new MongoPersistence();
}

@Bean
public MongoPersistence userMongoPersistence(){
	return new MongoPersistence();
}

//声明TransactionTemplate
@Bean
public TransactionTemplate orderTransactionTemplate(){
    return new TransactionTemplate();
}

@Bean
public TransactionTemplate userTransactionTemplate(){
    return new TransactionTemplate();
}
```

> 以上为伪代码

使用时,在order库进行业务发送和消费

```java
 @SendMqMessage(persistenceName = "orderMongoPersistence", transactionManager = "orderTransactionTemplate")
 public void send(){
 	...
 }

//消费
@ConsumeMqMessage(persistenceName = "orderMongoPersistence", transactionManager = "orderTransactionTemplate", messageClass = TestMessage.class, messageIdExpression = "#{message.eventId}")
public void consume(){
    ...
}
```

>以上为伪代码

或者使用注解派生

```java
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SendMqMessage(persistenceName = "orderMongoPersistence", transactionManager = "orderTransactionTemplate")
public @interface SendOrderMqMessage {

}
```

```java
@SendOrderMqMessage
public void send(){
	...
}
```

