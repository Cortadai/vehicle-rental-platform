package com.vehiclerental.common.messaging.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceContextHelperTest {

    @Test
    void returnsTraceparentWhenSpanActive() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("abc123");
        when(context.spanId()).thenReturn("def456");

        String result = TraceContextHelper.currentTraceparent(tracer);

        assertThat(result).isEqualTo("00-abc123-def456-01");
    }

    @Test
    void returnsNullWhenTracerIsNull() {
        assertThat(TraceContextHelper.currentTraceparent(null)).isNull();
    }

    @Test
    void returnsNullWhenNoCurrentSpan() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        assertThat(TraceContextHelper.currentTraceparent(tracer)).isNull();
    }

    @Test
    void returnsNullWhenContextHasNullTraceId() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn(null);

        assertThat(TraceContextHelper.currentTraceparent(tracer)).isNull();
    }
}
