package me.scf37.jmxhttp;

import me.scf37.jmxhttp.impl.DumbHttpServer;
import me.scf37.jmxhttp.impl.JmxHttpRequestHandler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;

public class JmxHttpServer {
    private static JmxHttpRequestHandler defaultHandler;

    public synchronized static CompletableFuture<byte[]> serve(byte[] request) {
        if (defaultHandler == null) {
            defaultHandler = new JmxHttpRequestHandler(
                    ManagementFactory.getPlatformMBeanServer(),
                    JmxHttpServer.class.getClassLoader()
            );
        }

        return defaultHandler.handle(request);
    }

    public static void startHttpServer(int port) throws IOException {
        new DumbHttpServer().serve(port);
    }
}
