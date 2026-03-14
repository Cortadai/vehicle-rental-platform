package com.vehiclerental.common.messaging.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

public final class TraceContextHelper {

    private TraceContextHelper() {
    }

    public static String currentTraceparent(Tracer tracer) {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        TraceContext context = currentSpan.context();
        if (context == null || context.traceId() == null || context.spanId() == null) {
            return null;
        }
        return String.format("00-%s-%s-01", context.traceId(), context.spanId());
    }
}
