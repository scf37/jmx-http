package me.scf37.jmxhttp.common.command;

import java.io.IOException;

import javax.management.MBeanServerConnection;

public final class GetDefaultDomain implements Command<String> {

  @Override
  public String execute(MBeanServerConnection connection, NotificationRegistry notificationRegistry) throws IOException {
    return connection.getDefaultDomain();
  }

}
