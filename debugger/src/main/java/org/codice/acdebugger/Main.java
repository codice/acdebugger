package org.codice.acdebugger;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import org.codice.acdebugger.breakpoints.AccessControlContextCheckBreakpointProcessor;
import org.codice.acdebugger.breakpoints.BackdoorProcessor;
import org.codice.acdebugger.impl.Debugger;

public class Main {
  public static void main(String[] args) throws Exception {
    String transport = "dt_socket";
    String host = "localhost";
    String port = "5005";
    long timeout = 10;
    boolean wait = false;
    boolean continuous = false;
    boolean dumping = false;
    boolean granting = false;
    boolean service = false;
    boolean doPrivileged = true;

    if (args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "-admin":
            doPrivileged = false;
            break;
          case "-continuous":
            continuous = true;
            break;
          case "-dump":
            dumping = true;
            break;
          case "-grant":
            granting = true;
            break;
          case "-service":
            service = true;
            break;
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
            if ((i + 1 <= args.length - 1) && !args[i + 1].startsWith("-")) {
              try {
                timeout = Long.valueOf(args[++i]);
                System.out.println("Set wait period to: " + timeout);
              } catch (NumberFormatException e) {
                System.err.println("Must use a decimal value in minutes for timeout");
              }
            }
            wait = true;
            break;
        }
      }
    }
    final long startTime = System.currentTimeMillis();
    Debugger debugger = null;

    // attach to VM
    while (debugger == null) {
      try {
        debugger = new Debugger(transport, host, port).attach();
      } catch (ConnectException e) {
        System.err.println("Unable to connect to " + host + ":" + port + " over " + transport);
        if (!wait
            || (System.currentTimeMillis() > startTime + TimeUnit.MINUTES.toMillis(timeout))) {
          System.exit(1);
        } else {
          TimeUnit.SECONDS.sleep(30);
        }
      }
    }
    // configure options
    debugger.setContinuous(continuous);
    debugger.setDumping(dumping);
    debugger.setGranting(granting);
    debugger.setMonitoringService(service);
    debugger.setDoPrivilegedBlocks(doPrivileged);

    // registering breakpoints
    debugger.add(new BackdoorProcessor());
    debugger.add(new AccessControlContextCheckBreakpointProcessor());
    // debugger.add(new ImpliesBreakpointProcessor()); // This slows the system to a crawl :-(

    debugger.loop();
  }
}
