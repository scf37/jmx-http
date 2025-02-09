package me.scf37.jmxhttp.impl;

import me.scf37.jmxhttp.common.command.ClassLoaderObjectInputStream;
import me.scf37.jmxhttp.common.command.Command;
import me.scf37.jmxhttp.common.http.Registration;
import me.scf37.jmxhttp.common.http.UnknownCorrelationIdException;

import javax.management.JMException;
import javax.management.MBeanServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.scf37.jmxhttp.common.http.HttpConstant.*;

/**
 * jmx-http request handler.
 * Takes request payload, responds with async response payload
 */
public class JmxHttpRequestHandler {
    private static final CompletableFuture<byte[]> HUMAN_RESPONSE =
            CompletableFuture.completedFuture("This is jmx-http endpoint. Use JMX client with url like: service:jmx:http://host:port/jmx".getBytes());
    private final MBeanServer server;
    private final ClassLoader classLoader;
    private final Random correlationIdGenerator = new Random();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private Map<Long, Correlation> correlations = new HashMap<>();

    public JmxHttpRequestHandler(MBeanServer server, ClassLoader classLoader) {
        this.server = server;
        this.classLoader = classLoader;
    }

    public CompletableFuture<byte[]> handle(byte[] request) {
        try {
            return doHandle(request);
        } catch (Exception e) {
            try {
                return CompletableFuture.completedFuture(writeObject(e));
            } catch (Exception ee) {
                try {
                    return CompletableFuture.completedFuture(writeObject(new RuntimeException("failed to serialize exception: " + e)));
                } catch (IOException ex) {
                    // we did everything we could
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private CompletableFuture<byte[]> doHandle(byte[] request) throws Exception {
        if (request.length < 4 + 4 + 8) {
            return HUMAN_RESPONSE;
        }

        DataInputStream is = new DataInputStream(new ByteArrayInputStream(request));
        int magic = is.readInt();
        if (magic != ACTION_MAGIC) {
            return CompletableFuture.completedFuture("This is jmx-http endpoint.".getBytes());
        }

        int action = is.readInt();
        long correlationId = is.readLong();

        switch (action)  {
            case ACTION_LISTEN:
                return handleListen(correlationId);
            case ACTION_REGISTER:
                return CompletableFuture.completedFuture(handleRegister());
            case ACTION_COMMAND:
                return CompletableFuture.completedFuture(handleCommand(is, correlationId));
            case ACTION_CONNECTION_ID:
                return CompletableFuture.completedFuture(handleGetConnectionId(correlationId));
        }

        return CompletableFuture.completedFuture("Invalid command - bad client.".getBytes());

    }

    private byte[] handleGetConnectionId(long correlationId) throws IOException {
        return writeObject(correlationId);
    }

    private byte[] handleCommand(InputStream is, long correlationId) throws IOException, JMException, ClassNotFoundException {
        Correlation correlation = getCorrelation(correlationId);

        Command<?> command = readObject(is);
        return writeObject(command.execute(server, correlation.getNotificationRegistry()));
    }

    private byte[] handleRegister() throws IOException {
        long correlationId = newCorrelation();

        return writeObject(new Registration(correlationId));
    }

    private CompletableFuture<byte[]> handleListen(long correlationId) {
        Correlation c = getCorrelation(correlationId);
        CompletableFuture<byte[]> notifications = c.fetchNotifications().thenCompose(list -> {
            try {
                return CompletableFuture.completedFuture(writeObject(list));
            } catch (IOException e) {
                CompletableFuture<byte[]> f = new CompletableFuture<>();
                f.completeExceptionally(e);
                return f;
            }
        });

        CompletableFuture<byte[]> result = new CompletableFuture<>();

        notifications.whenComplete((res, err) -> {
            if (res != null) {
                result.complete(res);
            } else {
                result.completeExceptionally(err);
            }
        });

        timer.schedule(() -> {
            if (!result.isDone()) {
                try {
                    result.complete(writeObject(Collections.emptyList()));
                } catch (IOException e) {
                    // should never happen
                    result.complete(new byte[0]);
                }
            }
        }, 30, TimeUnit.SECONDS);

        return result;
    }

    private byte[] writeObject(Object response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(ACTION_MAGIC);
        ObjectOutputStream os = new ObjectOutputStream(dos);
        os.writeObject(response);
        os.close();

        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private <R> R readObject(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ClassLoaderObjectInputStream(is, classLoader);

        return (R) ois.readObject();
    }

    private synchronized long newCorrelation() {
        long correlationId = correlationIdGenerator.nextLong();
        Correlation c = new Correlation(server);

        correlations.put(correlationId, c);

        return correlationId;
    }

    private synchronized Correlation getCorrelation(long correlationId) {
        Correlation correlation = correlations.get(correlationId);
        if (correlation == null) {
            throw new UnknownCorrelationIdException();
        }

        correlation.resetAccessTime();

        purgeExpiredCorrelations();

        return correlation;
    }

    private void purgeExpiredCorrelations() {
        List<Long> expiredCorrelations = new ArrayList<>();

        for (Map.Entry<Long, Correlation> e : correlations.entrySet()) {
            if (e.getValue().expired()) {
                expiredCorrelations.add(e.getKey());
            }
        }

        expiredCorrelations.forEach(id -> {
            correlations.remove(id).close();
        });
    }
}
