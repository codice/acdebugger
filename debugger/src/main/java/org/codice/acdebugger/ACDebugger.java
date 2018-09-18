/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.acdebugger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.net.ConnectException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.codice.acdebugger.breakpoints.AccessControlContextCheckBreakpointProcessor;
import org.codice.acdebugger.breakpoints.BackdoorProcessor;
import org.codice.acdebugger.cli.PropertiesVersionProvider;
import org.codice.acdebugger.impl.Debugger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  description = "Purpose-built debugger for determining missing OSGi bundle security permissions.",
  name = "acdebugger",
  mixinStandardHelpOptions = true,
  versionProvider = PropertiesVersionProvider.class
)
public class ACDebugger implements Callable<Void> {
  @Option(
    names = {"-a", "--admin"},
    description =
        "Indicates the tool is being run for an admin. In such cases, the analysis won't be as extensive"
            + " since an administrator wouldn't be able to modify the code for example. At the moment, "
            + "it disables analyzing solutions that involve extending privileges in bundles using "
            + "doPrivileged() blocks. In the above example, only the second solution would have been "
            + "reported if this option had been provided. As such, this option should not be used by developers."
  )
  private boolean admin = false;

  @Option(
    names = {"-c", "--continuous"},
    description =
        "Specifies to run in continuous mode where the debugger will tell the VM not to fail on any security"
            + " failures detected and report on all failures found."
  )
  private boolean continuous = false;

  @Option(
    names = {"-d", "--debug"},
    description =
        "Additional information about detected security failures such as stack traces and bundle information "
            + "will be printed along with solutions."
  )
  private boolean debug = false;

  @Option(
    names = {"-g", "--grant"},
    description =
        "When specified, the debugger will use the backdoor and a registered ServicePermission service to "
            + "temporarily grant permissions for detected security failures which after analysis yields a "
            + "single solution. This is only temporary and will not survive a restart of the VM but will "
            + "prevent any further failures that would otherwise not be if the permission(s) were defined. "
            + "It also tends to slow down the system since the OSGi permission cache ends up being cleared each time."
  )
  private boolean granting = false;

  @Option(
    names = {"-s", "--service"},
    description =
        "Specifies that a breakpoint should be added in Eclipse's Service Registry to detect internal "
            + "security checks done for given bundles before dispatching service events. These failures "
            + "are analyzed and reported as normal security check failures. This option tends to slow down "
            + "the system a bit as the debugger is invoked for all checks and not just when a failure is "
            + "about to be reported."
  )
  private boolean service = false;

  @Option(
    names = {"-t", "--transport"},
    description =
        "Specifies the transport to use when connecting to the VM. (default: ${DEFAULT-VALUE})"
  )
  private String transport = "dt_socket";

  @Option(
    names = {"-H", "--host"},
    description =
        "Specifies the host or IP where the VM to attach to is located. (default: ${DEFAULT-VALUE})"
  )
  private String host = "localhost";

  @Option(
    names = {"-p", "--port"},
    description =
        "Specifies the port number the VM is awaiting debuggers to connect to. (default:${DEFAULT-VALUE})"
  )
  private String port = "5005";

  @Option(
    names = {"-w", "--wait"},
    description =
        "Indicates to wait for a connection. To specify the timeout value use with the '--timeout' option."
  )
  private boolean wait = false;

  @Option(
    names = {"--timeout"},
    description =
        "Only applies when the '--wait' option is used. Sets the maximum number of minutes to wait. (default: ${DEFAULT-VALUE})"
  )
  private long timeout = 10;

  @Override
  @SuppressWarnings("squid:S106" /* this is a console application */)
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
          await().atMost(30, SECONDS);
        }
      }
    }
    // configure options
    debugger.setContinuous(continuous);
    debugger.setDebug(debug);
    debugger.setGranting(granting);
    debugger.setMonitoringService(service);
    debugger.setDoPrivilegedBlocks(!admin);

    // registering breakpoints
    debugger.add(new BackdoorProcessor());
    debugger.add(new AccessControlContextCheckBreakpointProcessor());
    // debugger.add(new ImpliesBreakpointProcessor()); // This slows the system to a crawl :-(

    debugger.loop();
    return null;
  }
}
