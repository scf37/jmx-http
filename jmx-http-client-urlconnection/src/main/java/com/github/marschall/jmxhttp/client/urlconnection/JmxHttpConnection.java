package com.github.marschall.jmxhttp.client.urlconnection;

import com.github.marschall.jmxhttp.common.command.AddNotificationListener;
import com.github.marschall.jmxhttp.common.command.AddNotificationListenerRemote;
import com.github.marschall.jmxhttp.common.command.Command;
import com.github.marschall.jmxhttp.common.command.CreateMBean;
import com.github.marschall.jmxhttp.common.command.GetAttribute;
import com.github.marschall.jmxhttp.common.command.GetAttributes;
import com.github.marschall.jmxhttp.common.command.GetDefaultDomain;
import com.github.marschall.jmxhttp.common.command.GetDomains;
import com.github.marschall.jmxhttp.common.command.GetMBeanCount;
import com.github.marschall.jmxhttp.common.command.GetMBeanInfo;
import com.github.marschall.jmxhttp.common.command.GetObjectInstance;
import com.github.marschall.jmxhttp.common.command.Invoke;
import com.github.marschall.jmxhttp.common.command.IsInstanceOf;
import com.github.marschall.jmxhttp.common.command.IsRegistered;
import com.github.marschall.jmxhttp.common.command.QueryMBeans;
import com.github.marschall.jmxhttp.common.command.QueryNames;
import com.github.marschall.jmxhttp.common.command.RemoveNotificationListener;
import com.github.marschall.jmxhttp.common.command.RemoveNotificationListenerRemote;
import com.github.marschall.jmxhttp.common.command.SetAttribute;
import com.github.marschall.jmxhttp.common.command.SetAttributes;
import com.github.marschall.jmxhttp.common.command.UnregisterMBean;
import com.github.marschall.jmxhttp.common.http.RemoteNotification;
import com.github.marschall.jmxhttp.common.http.UnknownCorrelationIdException;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


final class JmxHttpConnection implements MBeanServerConnection, Closeable {

  private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

  private final ConnectionNotifier notifier;
  private final Thread pollerThread;

  private final NotificationListenerSupport listeners = new NotificationListenerSupport();
  private final JmxHttpClient httpClient;
  private final long correlationId;
  private volatile boolean closed;

  JmxHttpConnection(JmxHttpClient httpClient, long correlationId, ConnectionNotifier notifier) {
    this.notifier = notifier;
    this.httpClient = httpClient;
    this.correlationId = correlationId;
    this.pollerThread = new Thread(this::listenLoop, "jmx-http-poll-thread");
    this.pollerThread.start();
  }

  @Override
  public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return send(new CreateMBean(className, name, null, null, null));
      // catch blocks here and below mimic RMI connector stub catch blocks
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return send(new CreateMBean(className, name, loaderName, null, null));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    try {
      return send(new CreateMBean(className, name, null, params, signature));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
    try {
      return send(new CreateMBean(className, name, loaderName, params, signature));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (ReflectionException | InstanceAlreadyExistsException | MBeanException | NotCompliantMBeanException | InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
    try {
      send(new UnregisterMBean(name));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | MBeanRegistrationException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
    try {
      return send(new GetObjectInstance(name));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
    try {
      return send(new QueryMBeans(name, query));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
    try {
      return send(new QueryNames(name, query));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public boolean isRegistered(ObjectName name) throws IOException {
    try {
      return send(new IsRegistered(name));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public Integer getMBeanCount() throws IOException {
    try {
      return send(new GetMBeanCount());
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    try {
      return send(new GetAttribute(name, attribute));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return send(new GetAttributes(name, attributes));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
    try {
      send(new SetAttribute(name, attribute));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
    try {
      return send(new SetAttributes(name, attributes));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    try {
      return send(new Invoke(name, operationName, params, signature));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public String getDefaultDomain() throws IOException {
    try {
      return send(new GetDefaultDomain());
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public String[] getDomains() throws IOException {
    try {
      return send(new GetDomains());
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
    long listenerId = listeners.registerListener(listener);
    Long handbackId = listeners.registerHandback(handback);
    try {
      send(new AddNotificationListenerRemote(name, listenerId, filter, handbackId));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
    try {
      send(new AddNotificationListener(name, listener, filter, handback));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      send(new RemoveNotificationListener(name, listener));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    try {
      send(new RemoveNotificationListener(name, listener, filter, handback));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void removeNotificationListener(ObjectName name, NotificationListener listener) throws IOException, InstanceNotFoundException, ListenerNotFoundException {
    long listenerId = listeners.getListenerId(listener);
    try {
      send(new RemoveNotificationListenerRemote(name, listenerId));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws IOException, InstanceNotFoundException, ListenerNotFoundException {
    long listenerId = listeners.getListenerId(listener);
    Long handbackId = listeners.getHandbackId(handback);
    try {
      send(new RemoveNotificationListenerRemote(name, listenerId, filter, handbackId));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
    try {
      return send(new GetMBeanInfo(name));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException | IntrospectionException | ReflectionException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
    try {
      return send(new IsInstanceOf(name, className));
    } catch (RuntimeException | IOException e) {
      throw e;
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new java.rmi.UnexpectedException("undeclared checked exception", e);
    }
  }

  @Override
  public void close() {
    this.pollerThread.interrupt();
    closed = true;
  }

  private <R> R send(Command<R> command) throws Exception {
    if (closed) {
      throw new IOException("Closed");
    }
      try {
          return httpClient.send(command, correlationId);
      } catch (UnknownCorrelationIdException e) {
          notifier.notifyUnexpectedError(e);
          throw new IOException(e);
      }
  }

  private void listenLoop() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        List<RemoteNotification> notifications = httpClient.pollNotifications(correlationId);
        for (RemoteNotification notification : notifications) {
          listeners.sendNotification(notification.getNotification(), notification.getListenerId(), notification.getObjectId());
        }
      } catch (UnknownCorrelationIdException e) {
        // session is dead :-/
          notifier.notifyUnexpectedError(e);
        break;
      } catch (Exception e) {
          // wait a bit and try to reconnect
          try {
              Thread.sleep(1000);
          } catch (InterruptedException ex) {
              break;
          }
       }
    }
  }

  String getConnectionId() throws IOException {
    return "http-jmx-connection-" + httpClient.getConnectionId(correlationId);
  }
}
