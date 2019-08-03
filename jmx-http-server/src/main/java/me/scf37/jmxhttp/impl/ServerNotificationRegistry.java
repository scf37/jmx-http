package me.scf37.jmxhttp.impl;

import com.github.marschall.jmxhttp.common.command.NotificationRegistry;
import com.github.marschall.jmxhttp.common.http.RemoteNotification;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bridges Command execution to mbean server listeners to notifications.
 *
 * It is closeable, unregistering its listeners on close
 */
class ServerNotificationRegistry implements NotificationRegistry, Closeable {
    private final MBeanServer server;
    private final Consumer<RemoteNotification> notificationHandler;

    private Map<Long, ServerNotificationListener> listeners = new HashMap<>();

    public ServerNotificationRegistry(MBeanServer server, Consumer<RemoteNotification> notificationHandler) {
        this.server = server;
        this.notificationHandler = notificationHandler;
    }

    @Override
    public synchronized void addNotificationListener(ObjectName name, long listenerId, NotificationFilter filter, Long handbackId) throws IOException, InstanceNotFoundException {
        ServerNotificationListener listener = new ServerNotificationListener(listenerId, name);

        server.addNotificationListener(name, listener, filter, handbackId);
        listeners.put(listenerId, listener);
    }

    @Override
    public synchronized void removeNotificationListener(ObjectName name, long listenerId) throws IOException, ListenerNotFoundException, InstanceNotFoundException {
        NotificationListener listener = listeners.get(listenerId);
        if (listener != null) {
            server.removeNotificationListener(name, listener);
        }
    }

    @Override
    public synchronized void removeNotificationListener(ObjectName name, long listenerId, NotificationFilter filter, Long objectId) throws IOException, ListenerNotFoundException, InstanceNotFoundException {
        NotificationListener listener = listeners.get(listenerId);
        if (listener != null) {
            server.removeNotificationListener(name, listener, filter, objectId);
        }
    }

    @Override
    public synchronized void close() {
        listeners.values().forEach(ServerNotificationListener::unregister);
    }

    private class ServerNotificationListener implements NotificationListener {
        private final long listenerId;
        private final ObjectName objectName;

        public ServerNotificationListener(
                long listenerId,
                ObjectName objectName
        ) {
            this.listenerId = listenerId;
            this.objectName = objectName;
        }

        @Override
        public void handleNotification(Notification notification, Object handbackId) {
            notificationHandler.accept(new RemoteNotification(notification, listenerId, (Long)handbackId));
        }

        public void unregister() {
            try {
                server.removeNotificationListener(objectName, this);
            } catch (Exception e) {
                System.out.println("listener unreg failed: " + e);
            }
        }
    }
}