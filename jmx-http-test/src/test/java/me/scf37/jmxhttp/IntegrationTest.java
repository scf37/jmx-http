package me.scf37.jmxhttp;

import org.junit.Assert;
import org.junit.Test;

import javax.management.MBeanServerConnection;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class IntegrationTest {
    @Test
    public void testJmxClientAndServer() throws Exception {
        JmxHttpServer.startHttpServer(7778);

        ManagementFactory.getPlatformMBeanServer().registerMBean(new Failure(), new ObjectName(":type=Failure,name=Failure"));

        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:http://localhost:7778/jmx");
        try (JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceURL, null)) {
            System.out.println("connecting...");
            connector.connect();
            System.out.println("connected");
            AtomicInteger notificationCount = new AtomicInteger();
            NotificationListener listener = (notification, handback) -> {
                notificationCount.incrementAndGet();
            };
            connector.addConnectionNotificationListener(listener, null, null);
            MBeanServerConnection connection = connector.getMBeanServerConnection();

            System.out.println("mbeans=" + connection.getMBeanCount());
            System.out.println("domain=" + connection.getDefaultDomain());
            connector.removeConnectionNotificationListener(listener);
            ObjectName name = new ObjectName("java.lang:type=GarbageCollector,name=PS MarkSweep");
            connection.addNotificationListener(name, listener, null, null);
            System.gc();
            Thread.sleep(200);
            connection.removeNotificationListener(name, listener);

            Assert.assertTrue(notificationCount.get() > 0);
        }
    }

    public interface FailureMBean {
        String getThrowingProp();

        Object getFailureProp();

        String throwingFun();

        Object failureFun();
    }

    public static class Failure implements FailureMBean {
        public String getThrowingProp() {
             throw new RuntimeException("booo");
        }

        public Object getFailureProp() {
            return this;
        }

        public String throwingFun() {
            throw new RuntimeException("booo");
        }

        public Object failureFun() {
            return this;
        }
    }

}
