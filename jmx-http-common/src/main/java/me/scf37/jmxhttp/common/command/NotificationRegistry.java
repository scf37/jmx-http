package me.scf37.jmxhttp.common.command;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

public interface NotificationRegistry {

  void addNotificationListener(ObjectName name, long listenerId, NotificationFilter filter, Long handbackId) throws IOException, InstanceNotFoundException;
  
  void removeNotificationListener(ObjectName name, long listenerId) throws IOException, ListenerNotFoundException, InstanceNotFoundException;

  void removeNotificationListener(ObjectName name, long listenerId, NotificationFilter filter, Long objectId) throws IOException, ListenerNotFoundException, InstanceNotFoundException;
  
}
