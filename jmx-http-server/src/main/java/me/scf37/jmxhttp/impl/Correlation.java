package me.scf37.jmxhttp.impl;

import me.scf37.jmxhttp.common.command.NotificationRegistry;
import me.scf37.jmxhttp.common.http.RemoteNotification;

import javax.management.MBeanServer;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Correlation - connection-scoped set of mbean listeners and machinery to manage them
 */
class Correlation implements Closeable {
    private static final Duration CORRELATION_EXPIRATION_DURATION = Duration.ofMinutes(5);

    private final ServerNotificationRegistry notificationRegistry;

    private Instant lastAccessed = Instant.now();
    private List<RemoteNotification> pendingNotifications = new ArrayList<>();
    private CompletableFuture<List<RemoteNotification>> pendingFuture;

    public Correlation(MBeanServer server) {
        this.notificationRegistry = new ServerNotificationRegistry(server, Correlation.this::consumeNotification);
    }

    public NotificationRegistry getNotificationRegistry() {
        return notificationRegistry;
    }

    public synchronized void resetAccessTime() {
        lastAccessed = Instant.now();
    }

    public synchronized boolean expired() {
        return Instant.now().compareTo(lastAccessed.plus(CORRELATION_EXPIRATION_DURATION)) > 0;
    }

    public synchronized CompletableFuture<List<RemoteNotification>> fetchNotifications() {
        if (pendingNotifications.size() > 0) {
            List<RemoteNotification> result = pendingNotifications;
            pendingNotifications = new ArrayList<>();
            return CompletableFuture.completedFuture(result);
        }

        if (pendingFuture == null) {
            pendingFuture = new CompletableFuture<>();
        }

        return pendingFuture;
    }

    private synchronized void consumeNotification(RemoteNotification remoteNotification) {
        if (pendingFuture != null) {
            pendingFuture.complete(Collections.singletonList(remoteNotification));
            pendingFuture = null;
            return;
        }

        pendingNotifications.add(remoteNotification);
        if (pendingNotifications.size() > 5000) {
            pendingNotifications.clear();
        }
    }

    public synchronized void close() {
        notificationRegistry.close();
        if (pendingFuture != null) {
            pendingFuture.complete(Collections.emptyList());
        }
    }
}
