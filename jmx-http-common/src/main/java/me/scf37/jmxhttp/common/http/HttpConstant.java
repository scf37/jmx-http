package me.scf37.jmxhttp.common.http;

public final class HttpConstant {

  public static final int ACTION_MAGIC = 0xd1ab10;
  public static final int ACTION_REGISTER = 1;
  public static final int ACTION_LISTEN = 2;
  public static final int ACTION_COMMAND = 3;
  public static final int ACTION_CONNECTION_ID = 4;


  private HttpConstant() {
    throw new AssertionError("not instantiable");
  }

}
