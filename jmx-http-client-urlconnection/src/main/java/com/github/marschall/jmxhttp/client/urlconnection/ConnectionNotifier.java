package com.github.marschall.jmxhttp.client.urlconnection;

public interface ConnectionNotifier {
    void notifyConnectionOpen();
    void notifyConnectionClose();
    void notifyUnexpectedError(Exception ex);
}
