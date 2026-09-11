package com.flashsale.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enable Spring's asynchronous background task scheduling for outbox processing workers.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
