# Access Controller Debugger
Purpose-built debugger for determining missing OSGi bundle security permissions

To run, open project in IDE and run the `Main.main` method.

### Current implementation
The current implementation is rather weak, breaking on line 472 of
`AccessControlContext` (the throws clause) and logging the data
for a single permission failure. Additionally, it does not currently
get the permission actions requested, only printing out the bundle context,
permission type, and requested resource(s).

The maven build can be used to produce an executable jar. The jar can
be run by doing: `java -jar .\access-control-debugger-1.0-SNAPSHOT-jar-with-dependencies.jar -host <hostname or IP> -port <port number> -wait <number of minutes to attempt to connect or empty for default>`

### Future iterations
Future implementations will

* Print out any specific permission actions
* Move the breakpoint further up in the `checkPermission`
method in order to continue processing
* Clean and refactor code, extracting a utility library
* Provide a Kotlin implementation

