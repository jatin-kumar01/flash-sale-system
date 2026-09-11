package com.flashsale.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTracePropagator {

    private final Tracer tracer;

    /**
     * Injects the active traceId and spanId into Kafka message headers.
     */
    public void injectTraceContext(Headers headers) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && currentSpan.context() != null) {
            String traceId = currentSpan.context().traceId();
            String spanId = currentSpan.context().spanId();

            headers.add(new RecordHeader(TracingConstants.MDC_TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8)));
            headers.add(new RecordHeader(TracingConstants.MDC_SPAN_ID, spanId.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Extracts traceId from Kafka headers for logging context.
     */
    public String extractTraceId(Headers headers) {
        var header = headers.lastHeader(TracingConstants.MDC_TRACE_ID);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
