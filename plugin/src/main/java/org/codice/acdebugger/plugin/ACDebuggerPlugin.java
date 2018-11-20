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
package org.codice.acdebugger.plugin;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codice.acdebugger.Main;

/**
 * Maven plugin that starts the AC Debugger in a separate thread during a maven build. Default goal
 * is on the 'pre-integration-test' phase but can be overridden in its configuration
 *
 * <p>can manually start the plugin by running: mvn acdebugger:start when added to a project
 *
 * <p>Important: The parameters are a direct copy of the ones in {@link
 * org.codice.acdebugger.ACDebugger} and should be kept in sync
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class ACDebuggerPlugin extends AbstractMojo {
  @Parameter private boolean skip;

  @Parameter private boolean remoteDebugging;

  @Parameter(defaultValue = "localhost")
  private String host;

  @Parameter(defaultValue = "5005")
  private String port;

  @Parameter private boolean wait;

  @Parameter(defaultValue = "10")
  private int timeout;

  @Parameter private boolean reconnect;

  @Parameter private boolean continuous;

  @Parameter private boolean admin;

  @Parameter private boolean debug;

  @Parameter private boolean service;

  @Parameter private boolean fail;

  @Parameter private boolean grant;

  @Parameter(defaultValue = "true")
  private boolean osgi;

  @VisibleForTesting
  ACDebuggerPlugin(Boolean skip, Boolean remoteDebugging) {
    this.skip = skip;
    this.remoteDebugging = remoteDebugging;
  }

  // Maven plugins need a default constructor if another constructor is defined
  ACDebuggerPlugin() {}

  @Override
  public void execute() {
    if (skip || remoteDebugging) {
      getLog().info("skipping AC Debugger");
      return;
    }

    List<String> arguments = buildArguments();
    Runnable acdebugger = () -> Main.main(arguments.toArray(new String[0]));
    getLog()
        .info("Starting AC Debugger with the following options: " + String.join(" ", arguments));
    Thread thread = new Thread(acdebugger, "AC Debugger Maven Plugin");
    thread.setDaemon(true);
    thread.start();
  }

  @VisibleForTesting
  List<String> buildArguments() {
    List<String> arguments = new ArrayList<>();
    addParameter(arguments, "--host", host);
    addParameter(arguments, "--port", port);
    addParameter(arguments, "--wait", wait);
    addParameter(arguments, "--timeout", Integer.toString(timeout));
    addParameter(arguments, "--reconnect", reconnect);
    addParameter(arguments, "--continuous", continuous);
    addParameter(arguments, "--admin", admin);
    addParameter(arguments, "--debug", debug);
    addParameter(arguments, "--service", service);
    addParameter(arguments, "--fail", fail);
    addParameter(arguments, "--grant", grant);
    arguments.add("--osgi=" + osgi);

    return arguments;
  }

  private void addParameter(List<String> list, String param, String value) {
    list.add(param);
    list.add(value);
  }

  private void addParameter(List<String> list, String param, boolean value) {
    if (value) {
      list.add(param);
    }
  }
}
