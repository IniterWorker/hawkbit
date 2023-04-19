/**
 * Copyright (c) 2021 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.function.Supplier;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * A singleton bean which holds the tracer in order to create spans. It can be
 * used in beans not instantiated by spring e.g. JPA entities which cannot be
 * auto-wired.
 */
public final class TracerHolder {

    private static final TracerHolder SINGLETON = new TracerHolder();

    @Autowired
    private Tracer tracer;

    @Autowired(required = false)
    private TenantAware tenantAware;

    private TracerHolder() {
    }

    public static TracerHolder getInstance() {
        return SINGLETON;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public <T> T wrapInSpan(final Supplier<T> supplier, final String spanName) {
        return wrapInSpan(supplier, spanName, null);
    }

    public <T> T wrapInSpan(final Supplier<T> supplier, final String spanName, final SpanKind kind) {
        return wrapInSpan(supplier, spanName, kind, null);
    }

    public <T> T wrapInSpan(final Supplier<T> supplier, final String spanName, final SpanKind kind,
            final Attributes customAttributes) {
        final Span span = createSpan(spanName, kind);
        span.setAttribute("thread", Thread.currentThread().getName());
        if (tenantAware != null) {
            span.setAttribute("tenant", tenantAware.getCurrentTenant());
            span.setAttribute("user", tenantAware.getCurrentUsername());
            final String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
            final Integer transactionIsolationLevel = TransactionSynchronizationManager
                    .getCurrentTransactionIsolationLevel();
            span.setAttribute("transaction_name", transactionName != null ? transactionName : "unknown");
            span.setAttribute("transaction_isolation",
                    transactionIsolationLevel != null ? transactionIsolationLevel.toString() : "unknown");
        }
        span.setAllAttributes(customAttributes);

        try (final Scope scope = span.makeCurrent()) {
            return supplier.get();
        } catch (final Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    public Span createSpan(final String spanName, final SpanKind kind) {
        return tracer.spanBuilder(sanitizeSpanName(spanName)).setSpanKind(kind != null ? kind : SpanKind.INTERNAL)
                .startSpan();
    }

    private static String sanitizeSpanName(final String spanName) {
        return !StringUtils.isEmpty(spanName) ? spanName : "unknown";
    }

    public void wrapInSpan(final Runnable runnable, final String spanName) {
        wrapInSpan(runnable, spanName, null);
    }

    public void wrapInSpan(final Runnable runnable, final String spanName, final SpanKind kind) {
        wrapInSpan(runnable, spanName, kind, null);
    }

    public void wrapInSpan(final Runnable runnable, final String spanName, final SpanKind kind,
            final Attributes customAttributes) {
        wrapInSpan(() -> {
            runnable.run();
            return null;
        }, spanName, kind, customAttributes);
    }

    public <T> T wrapInChildSpan(final Supplier<T> supplier, final String spanName) {
        return wrapInChildSpan(supplier, spanName, null);
    }

    public <T> T wrapInChildSpan(final Supplier<T> supplier, final String spanName, final SpanKind kind) {
        return wrapInChildSpan(supplier, spanName, kind, null);
    }

    public <T> T wrapInChildSpan(final Supplier<T> supplier, final String spanName, final SpanKind kind,
            final Attributes customAttributes) {
        if (!Span.current().getSpanContext().isValid()) {
            return supplier.get();
        }

        return wrapInSpan(supplier, spanName, kind, customAttributes);
    }

    public void wrapInChildSpan(final Runnable runnable, final String spanName) {
        wrapInChildSpan(runnable, spanName, null);
    }

    public void wrapInChildSpan(final Runnable runnable, final String spanName, final SpanKind kind) {
        wrapInChildSpan(runnable, spanName, kind, null);
    }

    public void wrapInChildSpan(final Runnable runnable, final String spanName, final SpanKind kind,
            final Attributes customAttributes) {
        if (!Span.current().getSpanContext().isValid()) {
            runnable.run();
            return;
        }

        wrapInSpan(runnable, spanName, kind, customAttributes);
    }
}
