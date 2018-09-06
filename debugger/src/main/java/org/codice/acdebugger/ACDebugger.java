package org.codice.acdebugger;

import java.net.ConnectException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.codice.acdebugger.breakpoints.AccessControlContextCheckBreakpointProcessor;
import org.codice.acdebugger.breakpoints.BackdoorProcessor;
import org.codice.acdebugger.impl.Debugger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  description = "Purpose-built debugger for determining missing OSGi bundle security permissions.",
  name = "acdebugger",
  mixinStandardHelpOptions = true
)
public class ACDebugger implements Callable<Void> {

  @Option(
    names = {"-a", "--admin"},
    description =
        "Indicates the tool is being run for an admin. In such cases, the analysis won't be as extensive since an administrator wouldn't be able to modify the code for example. At the moment, it disables analyzing solutions that involve extending privileges in bundles using doPrivileged() blocks. In the above example, only the second solution would have been reported if this option had been provided. As such, this option should not be used by developers."
  )
  private boolean admin = false;

  @Option(
    names = {"-c", "--continuous"},
    description =
        "Specifies to run in continuous mode where the debugger will tell the VM not to fail on any security failures detected and report on all failures found."
  )
  private boolean continuous = false;

  @Option(
    names = {"-d", "--dump"},
    description =
        "Additional information about detected security failures such as stack traces and bundle information will be printed along with solutions."
  )
  private boolean dumping = false;

  @Option(
    names = {"-g", "--grant"},
    description =
        "When specified, the debugger will use the backdoor and a registered ServicePermission service to temporarily grant permissions for detected security failures which after analysis yields a single solution. This is only temporary and will not survive a restart of the VM but will prevent any further failures that would otherwise not be if the permission(s) were defined. It also tends to slow down the system since the OSGi permission cache ends up being cleared each time."
  )
  private boolean granting = false;

  @Option(
    names = {"-s", "--service"},
    description =
        "Specifies that a breakpoint should be added in Eclipse's Service Registry to detect internal security checks done for given bundles before dispatching service events. These failures are analyzed and reported as normal security check failures. This option tends to slow down the system a bit as the debugger is invoked for all checks and not just when a failure is about to be reported."
  )
  private boolean service = false;

  @Option(
    names = {"-t", "--transport"},
    description = "Specifies the transport to use when connecting to the VM."
  )
  String transport = "dt_socket";

  @Option(
    names = {"-H", "--host"},
    description = "Specifies the host or IP where the VM to attach to is located."
  )
  String host = "localhost";

  @Option(
    names = {"-p", "--port"},
    description = "Specifies the port number the VM is awaiting debuggers to connect to."
  )
  String port = "5005";

  @Option(
    names = {"-w", "--wait"},
    description =
        "Indicates to wait for a connection. To specify the timeout value use with the '--wait-timeout' option."
  )
  boolean wait = false;

  @Option(
    names = {"--wait-timeout"},
    description =
        "Only applies when the '--wait' option is used. Sets the maximum number of minutes to wait"
  )
  long timeout = 10;

  @Override
  public Void call() throws Exception {

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
    if (admin) {
      debugger.setDoPrivilegedBlocks(false);
    } else {
      debugger.setDoPrivilegedBlocks(true);
    }

    // registering breakpoints
    debugger.add(new BackdoorProcessor());
    debugger.add(new AccessControlContextCheckBreakpointProcessor());
    // debugger.add(new ImpliesBreakpointProcessor()); // This slows the system to a crawl :-(

    debugger.loop();
    return null;
  }
}
