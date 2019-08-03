package me.scf37.jmxhttp.client.urlconnection;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

class  NotificationListenerSupport {
    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private static final long NULL_ID = -1L;

    private final Map<Long, NotificationListener> listeners = new HashMap<>();
    private final Map<NotificationListener, Long> listenersToId = new IdentityHashMap<>();
    private final Map<Long, Object> handbacks = new HashMap<>();
    private final Map<Object, Long> handbacksToId = new IdentityHashMap<>();

    private final AtomicLong handbackIdGenerator = new AtomicLong();
    private final AtomicLong listenerIdGenerator = new AtomicLong();

    public void sendNotification(Notification notification, long listenerId, Long handbackId) {
        try {
            NotificationListener listener = getListener(listenerId);
            Object handback = getHandback(handbackId);
            listener.handleNotification(notification, handback);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "exception occurred while delivering event to listener", e);
        }
    }

    private synchronized NotificationListener getListener(long listenerId) {
        NotificationListener listener = this.listeners.get(listenerId);
        if (listener == null) {
            System.out.println("no listener found for id: " + listenerId);
            throw new NoSuchElementException("no listener found for id: " + listenerId);
        }
        return listener;
    }

    public synchronized long registerListener(NotificationListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");

        // single listener can be registered multiple times
        // it is important to give same id to the same listener
        Long existingListenerId = listenersToId.get(listener);
        if (existingListenerId != null) {
            return existingListenerId;
        }

        long id = this.listenerIdGenerator.incrementAndGet();
        this.listeners.put(id, listener);
        this.listenersToId.put(listener, id);
        return id;
    }

    public synchronized Long registerHandback(Object handback) {
        if (handback == null) {
            return NULL_ID;
        }

        Long existingHandbackId = handbacksToId.get(handback);
        if (existingHandbackId != null) {
            return existingHandbackId;
        }

        long id = this.handbackIdGenerator.incrementAndGet();
        this.handbacks.put(id, handback);
        handbacksToId.put(handback, id);
        return id;
    }

    public synchronized Object getHandback(long handbackId) {
        if (handbackId == NULL_ID) {
            return null;
        }
        Object handback = this.handbacks.get(handbackId);
        if (handback == null) {
            System.out.println("no handback found for id: " + handbackId);
            throw new NoSuchElementException("no handback found for id: " + handbackId);
        }
        return handback;
    }

    public synchronized long getListenerId(NotificationListener listener) throws ListenerNotFoundException {
        Long id = this.listenersToId.get(listener);
        if (id == null) {
            throw new ListenerNotFoundException("listener: " + listener + " not found");
        }
        return id;
    }

    public synchronized long getHandbackId(Object handback) throws ListenerNotFoundException {
        if (handback == null) {
            return NULL_ID;
        }
        Long id = this.handbacksToId.get(handback);
        if (id == null) {
            throw new ListenerNotFoundException("handback: " + handback + " not found");
        }
        return id;
    }
}
