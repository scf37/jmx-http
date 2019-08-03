package com.github.marschall.jmxhttp.client.urlconnection;

import com.github.marschall.jmxhttp.common.http.Registration;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * The connector creates {@link MBeanServerConnection}s and manages
 * connection {@link NotificationListener}s.
 */
final class JmxHttpConnector implements JMXConnector {

  private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

  enum State {
    INITIAL,
    CONNECTED,
    CLOSED;
  }

  private final URL url;
  private final NotificationBroadcasterSupport connectionBroadcaster = new NotificationBroadcasterSupport();
  private final ConnectionNotifier notifier = new ConnectionNotifierImpl();

  private State state;
  private AtomicLong clientNotifSeqNo = new AtomicLong();
  private String connectionId;
  private JmxHttpConnection mBeanServerConnection;

  JmxHttpConnector(URL url) {
    this.url = url;
    this.state = State.INITIAL;
  }

  @Override
  public void connect() throws IOException {
    this.connect(null);
  }

  @Override
  public synchronized void connect(Map<String, ?> env) throws IOException {
    if (this.state == State.CONNECTED) {
      return;
    }
    if (this.state == State.CLOSED) {
      throw new IOException("already closed");
    }

    Optional<String> credentials = extractCredentials(env);
    // TODO better classloader
    JmxHttpClient httpClient = new JmxHttpClient(url, credentials, Thread.currentThread().getContextClassLoader());

    Registration registration;
    try {
      registration = httpClient.getRegistration();
    } catch (Exception e) {
      throw new IOException("Failed to connect to " + url +". Reason: " + e, e);
    }
    this.mBeanServerConnection = new JmxHttpConnection(httpClient, registration.getCorrelationId(), notifier);
    this.state = State.CONNECTED;

    this.connectionId = getConnectionId();

    notifier.notifyConnectionOpen();
  }


  private static Optional<String> extractCredentials(Map<String, ?> env) {
    if (env == null) {
      return Optional.empty();
    }
    Object possibleCredentials = env.get(CREDENTIALS);
    if (possibleCredentials instanceof String[]) {
      String[] credentialArray = (String[]) possibleCredentials;
      String username = credentialArray[0];
      String password = credentialArray[1];
      String userpass = username + ":" + password;
      String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes(ISO_8859_1)), ISO_8859_1);
      return Optional.of(basicAuth);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return this.mBeanServerConnection;
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
    throw new UnsupportedOperationException("delegation");
  }

  @Override
  public synchronized void close() throws IOException {
    if (this.state == State.CLOSED) {
      return;
    }

    try {
      if (this.mBeanServerConnection != null) {
        this.mBeanServerConnection.close();
      }
    } finally {
      if (this.connectionId != null) {
        notifier.notifyConnectionClose();
      }
      this.mBeanServerConnection = null;
      this.connectionId = null;
    }
  }

  @Override
  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
    if (listener == null)
      throw new NullPointerException("listener");
    connectionBroadcaster.addNotificationListener(listener, filter,
            handback);
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    if (listener == null)
      throw new NullPointerException("listener");
    connectionBroadcaster.removeNotificationListener(listener);
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
    if (listener == null)
      throw new NullPointerException("listener");
    connectionBroadcaster.removeNotificationListener(listener, filter,
            handback);
  }

  private void sendNotification(Notification n) {
    connectionBroadcaster.sendNotification(n);
  }

  @Override
  public synchronized String getConnectionId() throws IOException {

    if (state != State.CONNECTED) {
      throw new IOException("Not connected");
    }

    return mBeanServerConnection.getConnectionId();
  }

  private class ConnectionNotifierImpl implements ConnectionNotifier {

    @Override
    public void notifyConnectionOpen() {
      Notification connectedNotif =
              new JMXConnectionNotification(JMXConnectionNotification.OPENED,
                      this,
                      connectionId,
                      clientNotifSeqNo.incrementAndGet(),
                      "Successful connection",
                      null);
      sendNotification(connectedNotif);
    }

    @Override
    public void notifyConnectionClose() {
      Notification closedNotif =
              new JMXConnectionNotification(JMXConnectionNotification.CLOSED,
                      this,
                      connectionId,
                      clientNotifSeqNo.incrementAndGet(),
                      "Client has been closed",
                      null);
      sendNotification(closedNotif);
    }

    @Override
    public void notifyUnexpectedError(Exception ex) {
      final Notification failedNotif =
              new JMXConnectionNotification(
                      JMXConnectionNotification.FAILED,
                      this,
                      connectionId,
                      clientNotifSeqNo.incrementAndGet(),
                      "Failed to communicate with the server: " + ex.toString(),
                      ex);

      sendNotification(failedNotif);

      try {
        close();
      } catch (IOException e) {
      }
    }
  }

}
