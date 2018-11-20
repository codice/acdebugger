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
package org.codice.acdebugger.plugin

import org.apache.maven.plugin.testing.MojoRule
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

class ACDebuggerPluginSpec extends Specification {
    @Rule
    MojoRule rule = new MojoRule()

    def "make sure we can load the plugin"() {
        expect:
          loadPlugin() != null
    }

    def "make sure we skip when skip set to true"() {
        given:
          ACDebuggerPlugin plugin = Spy(constructorArgs: [true, false])
        when:
          plugin.execute()
        then:
          0 * plugin.buildArguments()
    }

    def "make sure we skip when remoteDebugging set to true"() {
        given:
          ACDebuggerPlugin plugin = Spy(constructorArgs: [false, true])
        when:
          plugin.execute()
        then:
          0 * plugin.buildArguments()
    }

    @Unroll
    def "test buildArguments() when #when_what"() {
        given:
          def plugin = loadPlugin(host: host, port: port, wait: wait, timeout: timeout, reconnect: reconnect, continuous: continuous,
                  admin: admin, debug: debug, service: service, fail: fail, grant: grant, osgi: osgi)
        when:
        def arguments = plugin.buildArguments()
        def expected = ['--host', host, '--port', port, parameter('--wait', wait), '--timeout', timeout,
                        parameter('--reconnect', reconnect), parameter('--continuous', continuous), parameter('--admin', admin),
                        parameter('--debug', debug), parameter('--service', service), parameter('--fail', fail),
                        parameter('--grant', grant), (String)"--osgi=$osgi"]
                .findAll{ it != null }
        then:
          arguments.containsAll(expected)
        where:
          when_what                     || host        | port   | wait  | timeout | reconnect | continuous | admin | debug | service | fail  | grant | osgi
          "all options are enabled"     || "localhost" | "1234" | true  | "100"   | true      | true       | true  | true  | true    | true  | true  | true
          "all options are disabled"    || "localhost" | "1234" | false | "0"     | false     | false      | false | false | false   | false | false | false
    }

    ACDebuggerPlugin loadPlugin(Map args = [:]) {
        def baseDir = new File("target/test-classes/project-to-test/")
        def project = rule.readMavenProject(baseDir)
        def properties = project.properties
        properties.setProperty("host", args.host ?: "localhost")
        properties.setProperty("port", args.port ?: "1234")
        properties.setProperty("wait", Boolean.toString((boolean)args.wait))
        properties.setProperty("timeout", args.timeout ?: "5")
        properties.setProperty("reconnect", Boolean.toString((boolean)args.reconnect))
        properties.setProperty("continuous", Boolean.toString((boolean)args.continuous))
        properties.setProperty("admin", Boolean.toString((boolean)args.admin))
        properties.setProperty("debug", Boolean.toString((boolean)args.debug))
        properties.setProperty("service", Boolean.toString((boolean)args.service))
        properties.setProperty("fail", Boolean.toString((boolean)args.fail))
        properties.setProperty("grant", Boolean.toString((boolean)args.grant))
        properties.setProperty("osgi", Boolean.toString(args.osgi == null ? true : args.osgi))
        properties.setProperty("skip", Boolean.toString((boolean)args.skip))
        properties.setProperty("remoteDebugging", Boolean.toString((boolean)args.remoteDebugging))


        return rule.lookupConfiguredMojo(project, "start") as ACDebuggerPlugin
    }

    String parameter(String name, boolean value) {
        return value ? name : null
    }
}