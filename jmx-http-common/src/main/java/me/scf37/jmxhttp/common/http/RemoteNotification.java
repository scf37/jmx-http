package me.scf37.jmxhttp.common.http;

import javax.management.Notification;
import java.io.Serializable;

public final class RemoteNotification implements Serializable {

  private final Notification notification;
  private final long listenerId;
  private final Long objectId;

  public RemoteNotification(Notification notification, long listenerId, Long objectId) {
    this.notification = notification;
    this.listenerId = listenerId;
    this.objectId = objectId;
  }

  public long getListenerId() {
    return listenerId;
  }

  public Notification getNotification() {
    return this.notification;
  }

  public Long getObjectId() {
    return this.objectId;
  }

}