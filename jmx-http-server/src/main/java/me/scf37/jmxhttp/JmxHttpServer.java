package me.scf37.jmxhttp;

import me.scf37.jmxhttp.impl.DumbHttpServer;
import me.scf37.jmxhttp.impl.JmxHttpRequestHandler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for jmx-http server API
 */
public class JmxHttpServer {
    private static JmxHttpRequestHandler defaultHandler;

    /**
     * HTTP request handler. Stick it into your favorite web server/framework.
     * Client will do binary POST requests to single URL.
     *
     * Concurrency note for synchronous servers: as client uses long polling,
     *  expect one thread to be fully occupied per client connected
     *
     * @param request HTTP request payload
     * @return HTTP response payload
     */
    public synchronized static CompletableFuture<byte[]> serve(byte[] request) {
        if (defaultHandler == null) {
            defaultHandler = new JmxHttpRequestHandler(
                    ManagementFactory.getPlatformMBeanServer(),
                    JmxHttpServer.class.getClassLoader()
            );
        }

        return defaultHandler.handle(request);
    }

    /**
     * Lazy man's JMX HTTP server. After calling this method, JMX becomes available under url
     * "service:jmx:http://{host}:{port}/jmx"
     *
     * @param port
     * @throws IOException
     */
    public static void startHttpServer(int port) throws IOException {
        new DumbHttpServer().serve(port);
    }

    private JmxHttpServer() {
    }
}
