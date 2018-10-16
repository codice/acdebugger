# Access Controller Debugger
Purpose-built debugger for determining missing security permissions in OSGi containers and non-OSGi VM.

### Current implementation
The current implementation will put a breakpoint on line 472 of
`java.security.AccessControlContext` (the throws clause), analyze and report possible solutions to the security failure. 
It will do so for a single failure and exit unless the `-continuous` option is used in which case it will let the VM continue as if no failures had occurred and report for all failures (unless they were already detected and reported).
The debugger also accounts for acceptable security failures. These are security failures that occurs in specific parts of the system which are deemed acceptable and for which no permissions need to be granted. 
This is typically the case when the code actually handles such exceptions appropriately without consequences. When such failures are detected, the debugger will simply ignore them and let them fail normally without reporting them (unless the --debug option is specified).

It also provides an option where it will put a breakpoint on line 1096 of `org.eclipse.osgi.internal.serviceregistry.ServiceRegistry` to detect all service permission checks done before the registry dispatches service events to other bundles. 
This option is not on by default as it can slow down the system a bit.

To run the debugger tool, open the debugger project in IDE and run the `org.codice.acdebuger.Main.main()` method. Options can be set in the corresponding IDE `Run/Debug Configuration`.

The maven build can be used to produce an executable jar. The jar can
be run by doing: `java -jar acdebugger-debugger-1.5-jar-with-dependencies.jar [options]>`

### Typical Output
For OSGi containers, the typical output will be:
```
AC Debugger: 0002 - Check permission failure for platform-migratable-api: java.io.FilePermission "${ddf.home.perm}security${/}configurations.policy", "read" {
AC Debugger:     Analyze the following 2 solutions and choose the best:
AC Debugger:     {
AC Debugger:         Add an AccessController.doPrivileged() block around:
AC Debugger:             platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:     }
AC Debugger:     {
AC Debugger:         Add the following permission block to default.policy:
AC Debugger:             grant codeBase "file:/platform-migratable-api" {
AC Debugger:                 permission java.io.FilePermission "${ddf.home.perm}security${/}configurations.policy", "read";
AC Debugger:             }
AC Debugger:     }
AC Debugger: }
```

For non-OSGi containers, the typical output will be:
```
AC Debugger: 0002 - Check permission failure for platform-migratable-api: java.io.FilePermission "${solr.solr.home}${/}server${/}modules", "read" {
AC Debugger:     Add the following permission blocks to the appropriate policy file:
AC Debugger:         grant codeBase "file:${solr.solr.home}${/}server${/}security${/}pro-grade-1.1.3.jar" {
AC Debugger:             permission java.io.FilePermission "${solr.solr.home}${/}server${/}modules", "read";
AC Debugger:         }
AC Debugger:         grant codeBase "file:${solr.solr.home}${/}server${/}server${/}start.jar" {
AC Debugger:             permission java.io.FilePermission "${solr.solr.home}${/}server${/}modules", "read";
AC Debugger:         }
AC Debugger: }
```

The above example is reporting the second analysis solutions block with 2 potential solutions.
It lists on the first line the bundle currently responsible (`platform-migratable-api`) for the failure and the corresponding permission(s).
You will also see a summary of all possible solutions to the problem.
These are sorted in order of priority but it is still up to the developer to assess which one is better suited in the current context.
As seen in the above example, the analysis gives priority to extending existing privileges to solve a security failure over introducing a permission to a new bundle.

### Options
The following debugger options are available:
* --help / -h
* --version / -V
* --host / -H `<hostname or IP>`
* --port / -p `<port number>`
* --wait / -w
* --timeout `<timeout>` (only applies when `--wait` is used)
* --reconnect / -r
* --continuous / -c
* --admin / -a
* --debug / -d
* --service / -s
* --fail / -f
* --grant / -g
* --osgi=`<osgi>`

#### --help / -h 
Prints out usage information and exit.

#### --version / -V
Prints version information and exit.

#### --host / -H `<hostname or IP>`
Specifies the host or IP where the VM to attach to is located
 
#### --port / -p `<port number>`
Specifies the port number the VM is awaiting debuggers to connect to

#### --wait / -w
Indicates to wait for a connection. The default timeout is 10 minutes.

#### --timeout `<timeout>`
Specified to change the default timeout when waiting for a connection; it indicates the maximum number of minutes to wait (defaults to 10 minutes).

#### --reconnect / -r
Indicates to attempt to reconnect automatically after the attached VM has disconnected  (--continuous must also be specified).

#### --continuous / -c
Specifies to run in continuous mode where the debugger will tell the VM not to fail on any security failures detected (unless --fail is specified) and report on all failures found.

#### --admin / -a
Indicates the tool is being run for an admin. In such cases, the analysis won't be as extensive since an administrator wouldn't be able to modify the code for example.
At the moment, it disables analyzing solutions that involve extending privileges in bundles using `doPrivileged()` blocks. 
In the above example, only the second solution would have been reported if this option had been provided. As such, this option should not be used by developers.

#### --debug / -d
Additional information about detected security failures such as stack traces and bundle information will be printed along with solutions.

Please refer to [this page](docs/debug.MD) for more information on the format of the output and for examples.

#### --service / -s
Specifies that a breakpoint should be added in Eclipse's Service Registry to detect internal security checks done for given bundles before dispatching service events. 
These failures are analyzed and reported as normal security check failures. This option tends to slow down the system a bit as the debugger is invoked for all checks and not just when a failure is about to be reported.

#### --fail / -f
When specified, the debugger will let security failures detected fail normally after reporting on all of them.

#### --grant / -g
When specified, the debugger will use the backdoor and a registered ServicePermission service to temporarily grant permissions for detected security failures which after analysis yields a single solution. 
This is only temporary and will not survive a restart of the VM but will prevent any further failures that would otherwise not be if the permission(s) were defined. 
It also tends to slow down the system since the OSGi permission cache ends up being cleared each time.

#### --osgi=`<osgi>`
Indicates the VM we are able to debug is an OSGi container. (default: true)
When debugging a non-OSGi container, the debugger will report the codesource location of domains instead of bundle names. 
 
### Modules
The following modules are defined:
* acdebugger-api
* acdebugger-backdoor
* acdebugger-debugger
* acdebugger-common
 
#### acdebugger-api
Defines a bundle that provides an interface for a permission service which is used by the backdoor bundle to temporarily grant missing permissions. This service should be registered by the VM.

#### acdebugger-backdoor
Defines a bundle that provides backdoor support to the debugger. It should be installed in the VM in order for the debugger to be more optimal.

#### acdebugger-debugger
Creates an executable jar with the debugger tool.

#### acdebugger-common
Defines a library of common classes used by both the backdoor and the debugger. This artifact is embedded inside the backdoor bundle.

### Future iterations
Future implementations will:
* Provide a user interface
* Provide a Kotlin implementation
* Enhance the backdoor bundle to actually intercept calls inside the VM rendering the debugging experience much more stable
