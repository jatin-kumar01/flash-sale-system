package com.flashsale.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        // Retry failed Kafka messages 3 times
        // with a 1-second delay between attempts.
        FixedBackOff backOff = new FixedBackOff(
                1000L,
                3L
        );

        return new DefaultErrorHandler(backOff);
    }
}