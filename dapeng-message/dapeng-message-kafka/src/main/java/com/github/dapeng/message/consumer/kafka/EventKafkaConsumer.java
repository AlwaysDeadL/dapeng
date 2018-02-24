package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.message.consumer.api.context.ConsumerContext;
import com.github.dapeng.message.event.serializer.KafkaMessageProcessor;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 描述: 作为事件总线 的消费者
 *
 * @author maple.lei
 * @date 2018年02月23日 下午4:26
 */
public class EventKafkaConsumer extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(EventKafkaConsumer.class);
    private List<ConsumerContext> customers = new ArrayList<>();

    private String groupId, topic;

    public EventKafkaConsumer(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
        init();
    }

    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_PORT;

    protected org.apache.kafka.clients.consumer.KafkaConsumer<Long, byte[]> consumer;

    public void init() {

        logger.info(new StringBuffer("[KafkaConsumer] [init] ")
                .append("kafkaConnect(").append(kafkaConnect)
                .append(") groupId(").append(groupId)
                .append(") topic(").append(topic).append(")").toString());

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConnect);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", LongDeserializer.class);
        props.put("value.deserializer", ByteArrayDeserializer.class);

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

    }

    @Override
    public void run() {

        try {
            logger.info("[KafkaConsumer][{}][run] ", groupId + ":" + topic);

            consumer.subscribe(Arrays.asList(topic));
            while (true) {
                ConsumerRecords<Long, byte[]> records = consumer.poll(100);
                for (ConsumerRecord<Long, byte[]> record : records) {
                    receive(record.value());
                }
            }
        } catch (Exception e) {
            logger.error("[KafkaConsumer][{}][run] " + e.getMessage(), groupId + ":" + topic, e);
        }
    }


    /**
     * Kafka Consumer接收到消息，调用方法消费消息
     *
     * @param message
     */
    private void receive(byte[] message) throws TException {

        logger.info("KafkaConsumer groupId({}) topic({}) 收到消息", groupId, topic);
        for (ConsumerContext customer : customers) {
            dealMessage(customer, message);
        }
    }

    /**
     * 添加一个订阅同一个topic的“客户端”,客户端可以理解为一个订阅消息的方法
     *
     * @param client
     */
    public void addCustomer(ConsumerContext client) {
        this.customers.add(client);
    }

    public List<ConsumerContext> getCustomers() {
        return customers;
    }

    public void setCustomers(List<ConsumerContext> customers) {
        this.customers = customers;
    }


    /**
     * 处理收到的消息
     *
     * @param customer
     * @param message
     * @throws TException
     */
    private void dealMessage(ConsumerContext customer, byte[] message) throws TException {

        SoaFunctionDefinition.Sync functionDefinition = (SoaFunctionDefinition.Sync) customer.getSoaFunctionDefinition();
        Object iface = customer.getIface();

        long count = new ArrayList<>(Arrays.asList(iface.getClass().getInterfaces()))
                .stream()
                .filter(m -> "org.springframework.aop.framework.Advised".equals(m.getName()))
                .count();

        Class<?> ifaceClass;
        try {
            ifaceClass = (Class) (count > 0 ? iface.getClass().getMethod("getTargetClass").invoke(iface) : iface.getClass());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ifaceClass = iface.getClass();
        }
        /**
         * 解码消息为event
         */
        KafkaMessageProcessor processor = new KafkaMessageProcessor();
        Object event = processor.dealMessage(message, iface.getClass().getClassLoader());

        Method method = ((SoaFunctionDefinition.Sync) functionDefinition).getClass().getDeclaredMethods()[1];
        Parameter[] parameters = method.getParameters();
        Object argsParam = null;
        for (Parameter param : parameters) {
            if (param.getType().getName().contains("args")) {
                try {
                    Constructor<?> constructor = param.getType().getConstructor(event.getClass());
                    argsParam = constructor.newInstance(event);
                } catch (NoSuchMethodException e) {
                    logger.error("当前方法参数不匹配: {}, 当前消息事件不匹配该方法", method.getName());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    logger.error(" failed to instance method: {}", method.getName());
                    logger.error("当前方法参数不匹配: {}", method.getName());
                }
            }
        }

        try {
            checkNotNull(argsParam);

            logger.info("{}收到kafka消息，执行{}方法", ifaceClass.getName(), functionDefinition.methodName);
            functionDefinition.apply(iface, argsParam);
            logger.info("{}收到kafka消息，执行{}方法完成", ifaceClass.getName(), functionDefinition.methodName);

        } catch (NullPointerException e) {
            logger.error("{}收到kafka消息，执行{}方法异常,msg: {}", ifaceClass.getName(), functionDefinition.methodName, e.getMessage());
        } catch (Exception e) {
            logger.error("{}收到kafka消息，执行{}方法异常", ifaceClass.getName(), functionDefinition.methodName);
            logger.error(e.getMessage(), e);
        }

    }

    private void checkNotNull(Object argsParam) {
        if (argsParam == null) {
            throw new NullPointerException("解析订阅方法参数错误，可能该方法不处理这个事件");
        }
    }


}
