package com.flashsale.common.tracing;

public final class TracingConstants {

    private TracingConstants() {}

    public static final String TRACE_PARENT_HEADER = "traceparent";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
}
