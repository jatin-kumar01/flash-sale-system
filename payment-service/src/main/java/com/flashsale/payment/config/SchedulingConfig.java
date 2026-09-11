package com.flashsale.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enable background task scheduling within payment-service for outbox polling loops.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
