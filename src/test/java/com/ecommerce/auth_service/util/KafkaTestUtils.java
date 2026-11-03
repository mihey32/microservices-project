package com.ecommerce.auth_service.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

public class KafkaTestUtils {
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    public static <T> List<T> readMessages(String bootstrapServers, String topic, Class<T> clazz,
                                           int expectedCount, Duration timeout) {
        return readMessages(bootstrapServers, topic, clazz, e -> true, expectedCount, timeout);
    }

    public static <T> List<T> readMessages(String bootstrapServers, String topic, Class<T> clazz, Predicate<T> filter, int expectedCount, Duration timeout) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<T> result = new ArrayList<>();

        long endTime = System.currentTimeMillis() + timeout.toMillis();


        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(topic));
            while (result.size() < expectedCount && System.currentTimeMillis() < endTime) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    T parsed = objectMapper.readValue(record.value(), clazz);
                    if (filter.test(parsed)) {
                        result.add(parsed);
                    }
                }
            }
        }
        return  result;
    }
}
