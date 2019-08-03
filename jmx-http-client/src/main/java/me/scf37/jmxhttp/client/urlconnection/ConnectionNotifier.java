package me.scf37.jmxhttp.client.urlconnection;

public interface ConnectionNotifier {
    void notifyConnectionOpen();
    void notifyConnectionClose();
    void notifyUnexpectedError(Exception ex);
}
