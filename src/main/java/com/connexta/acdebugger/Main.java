package com.connexta.acdebugger;

public class Main {
  private static final int THROWS_LINE = 472;

  public static void main(String[] args) throws Exception {
    PermissionDebugger debugger = new PermissionDebugger("dt_socket", "5005").attach();

    debugger.process("java.security.AccessControlContext", THROWS_LINE);
  }
}
