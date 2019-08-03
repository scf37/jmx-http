package me.scf37.jmxhttp.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.scf37.jmxhttp.JmxHttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplest HTTP server for those unfortunate souls missing HTTP endpoint in their application.
 */
public class DumbHttpServer {
    private static Executor httpExecutor = null;

    /**
     * Start JMX HTTP server on provided port.
     * Jmx endpoint URL will be "service:jmx:http://{host}:{port}/jmx"
     * @param port port to use
     * @throws IOException if port is already occupied
     */
    public void serve(int port) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/jmx", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int len;
                    byte[] buf = new byte[8192];
                    while ((len = httpExchange.getRequestBody().read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }

                    byte[] resp = JmxHttpServer.serve(os.toByteArray()).get();
                    httpExchange.sendResponseHeaders(200, resp.length);
                    httpExchange.getResponseBody().write(resp);
                    httpExchange.getResponseBody().close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        httpExchange.getResponseBody().close();
                    } catch (Exception e) { }
                }
            }
        });

        Executor executor;
        synchronized (this) {
            if (httpExecutor == null) {
                httpExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
                    private AtomicInteger i = new AtomicInteger();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("jmx-http-server-" + i.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                });
            }
            executor = httpExecutor;
        }

        server.setExecutor(executor);
        server.start();
    }
}
