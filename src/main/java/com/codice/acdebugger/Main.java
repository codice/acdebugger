package com.codice.acdebugger;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

public class Main {
  private static final int THROWS_LINE = 472;

  public static void main(String[] args) throws Exception {

    String transport = "dt_socket";
    String host = "localhost";
    String port = "5005";
    long timeout = 10;
    boolean wait = false;

    if (args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "-transport":
            transport = args[++i];
            break;
          case "-host":
            host = args[++i];
            break;
          case "-port":
            port = args[++i];
            break;
          case "-wait":
            if (i + 1 <= args.length - 1) {
              try {
                timeout = Long.valueOf(args[i + 1]);
                System.out.println("Set wait period to: " + timeout);
              } catch (NumberFormatException e) {
                System.err.println("Must use a decimal value in minutes for timeout");
              }
            }
            wait = true;
        }
      }
    }

    PermissionDebugger debugger = null;
    long startTime = System.currentTimeMillis();
    while (debugger == null) {
      try {
        debugger = new PermissionDebugger(transport, host, port).attach();
      } catch (ConnectException e) {
        System.err.println("Unable to connect to " + host + ":" + port + " over " + transport);
        if (!wait || System.currentTimeMillis() > startTime + TimeUnit.MINUTES.toMillis(timeout)) {
          System.exit(1);
        } else {
          TimeUnit.SECONDS.sleep(30);
        }
      }
    }

    debugger.process("java.security.AccessControlContext", THROWS_LINE);
  }
}
