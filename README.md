# Access Controller Debugger
Purpose-built debugger for determining missing OSGi bundle security permissions.

### Current implementation
The current implementation will put a breakpoint on line 472 of
`java.security.AccessControlContext` (the throws clause), analyze and report possible solutions to the security failure. 
It will do so for a single failure and exit unless the `-continuous` option is used in which case it will let the VM continue as if no failures had occurred and report for all failures (unless they were already detected and reported).
The debugger also accounts for acceptable security failures. These are security failures that occurs in specific parts of the system which are deemed acceptable and for which no permissions requires granting. 
This is typically the case when the code actually handles such exception appropriately without consequences. When such failures are detected, the debugger will simply ignore them and let them fail normally without reporting them (unless the --dump option is specified).

It also provides an option where it will put a breakpoint on line 1096 of `org.eclipse.osgi.internal.serviceregistry.ServiceRegistry` to detect all service permission checks done before the registry dispatches service events to other bundles. 
This option is not on by default as it can slow down the system a bit.

To run the debugger tool, open the debugger project in IDE and run the `org.codice.acdebuger.Main.main()` method. Options can be set in the corresponding IDE `Run/Debug Configuration`.

The maven build can be used to produce an executable jar. The jar can
be run by doing: `java -jar acdebugger-debugger-1.0-SNAPSHOT-jar-with-dependencies.jar [options]>`

### Typical Output
```
0002 - Check permission failure for platform-migratable-api: java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/configurations.policy", "read" {
    Analyze the following 2 solutions and choose the best:
    {
        Add an AccessController.doPrivileged() block around:
            platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
    }
    {
        Add the following permission to default.policy:
            grant codeBase "file:/platform-migratable-api" {
                permission java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/configurations.policy", "read";
            }
    }
}
```

The above example is reporting the second analysis solutions block with 2 potential solutions.
It lists on the first line the bundle currently responsible (`platform-migratable-api`) for the failure and the corresponding permission(s).
You will also see a summary of all possible solutions to the problem.
These are sorted in order of priority but it is still up to the developer to assess which one is better suited in the current context.
As seen in the above example, the analysis gives priority to extending existing privileges to solve a security failure over introducing a permission to a new bundle.

### Options
The following debugger options are available:
* --host `<hostname or IP>`
* --port `<port number>`
* --wait 
* --wait-timeout `[<timeout>]` (only applies when `--wait` is used)
* --continuous
* --admin
* --dump
* --service
* --grant
* --help
* --version

#### -host `<hostname or IP>`
Specifies the host or IP where the VM to attach to is located
 
#### -port `<port number>`
Specifies the port number the VM is awaiting debuggers to connect to

#### -wait `[<timeout>]`
Indicates to wait for a connection. The timeout is optional and indicates the maximum number of minutes to wait (defaults to 10 minutes).

#### -continuous
Specifies to run in continuous mode where the debugger will tell the VM not to fail on any security failures detected and report on all failures found.

#### -admin
Indicates the tool is being run for an admin. In such cases, the analysis won't be as extensive since an administrator wouldn't be able to modify the code for example.
At the moment, it disables analyzing solutions that involve extending privileges in bundles using `doPrivileged()` blocks. 
In the above example, only the second solution would have been reported if this option had been provided. As such, this option should not be used by developers.

#### -dump
Additional information about detected security failures such as stack traces and bundle information will be dumped along with solutions.

Here is an example of a dump:
```
 0136 - ACCESS CONTROL PERMISSION FAILURE
 ========================================
 Permission:
     java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
 Context:
      bundle-0
      org.apache.commons.io
  --> *platform-migration
 Stack:
      at bundle-0(java.security.AccessControlContext:472)
      at bundle-0(java.security.AccessController:884)
      at bundle-0(java.lang.SecurityManager:549)
      at bundle-0(java.lang.SecurityManager:888)
      at bundle-0(java.io.File:844)
      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
      at bundle-0(java.io.File:1291)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
  --> at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
     ----------------------------------------------------------
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
      at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
      at bundle-0(java.util.Iterator:116)
      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
      at bundle-0(java.util.stream.AbstractPipeline:481)
      at bundle-0(java.util.stream.AbstractPipeline:471)
      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
      at bundle-0(java.util.stream.AbstractPipeline:234)
      at bundle-0(java.util.stream.ReferencePipeline:510)
      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
      at bundle-0(java.util.concurrent.FutureTask:266)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
      at bundle-0(java.lang.Thread:748)
 
 OPTION 1
 --------
 Permission:
     java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
 Granting permission to bundles:
     platform-migration
 Extending privileges at:
     platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
 Context:
      bundle-0
      org.apache.commons.io
      platform-migration
 Stack:
      at bundle-0(java.security.AccessControlContext:472)
      at bundle-0(java.security.AccessController:884)
      at bundle-0(java.lang.SecurityManager:549)
      at bundle-0(java.lang.SecurityManager:888)
      at bundle-0(java.io.File:844)
      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
      at bundle-0(java.io.File:1291)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction))
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
     ----------------------------------------------------------
      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
      at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
      at bundle-0(java.util.Iterator:116)
      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
      at bundle-0(java.util.stream.AbstractPipeline:481)
      at bundle-0(java.util.stream.AbstractPipeline:471)
      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
      at bundle-0(java.util.stream.AbstractPipeline:234)
      at bundle-0(java.util.stream.ReferencePipeline:510)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
      at bundle-0(java.util.concurrent.FutureTask:266)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
      at bundle-0(java.lang.Thread:748)
 
 OPTION 2
 --------
 Permission:
     java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
 Granting permission to bundles:
     platform-migration
     platform-migratable-api
 Context:
      bundle-0
      org.apache.commons.io
      platform-migration
      platform-migratable-api
 Stack:
      at bundle-0(java.security.AccessControlContext:472)
      at bundle-0(java.security.AccessController:884)
      at bundle-0(java.lang.SecurityManager:549)
      at bundle-0(java.lang.SecurityManager:888)
      at bundle-0(java.io.File:844)
      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
      at bundle-0(java.io.File:1291)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
      at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
      at platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
     ----------------------------------------------------------
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
      at platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
      at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
      at bundle-0(java.util.Iterator:116)
      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
      at bundle-0(java.util.stream.AbstractPipeline:481)
      at bundle-0(java.util.stream.AbstractPipeline:471)
      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
      at bundle-0(java.util.stream.AbstractPipeline:234)
      at bundle-0(java.util.stream.ReferencePipeline:510)
      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
      at bundle-0(java.util.concurrent.FutureTask:266)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
      at bundle-0(java.lang.Thread:748)
 
 SOLUTIONS
 ---------
 {
     Add the following permission to the appropriate policy file:
         grant codeBase "file:/platform-migration" {
             permission java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read";
         }
     and add an AccessController.doPrivileged() block around:
         platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
 }
 {
     Add the following permission to the appropriate policy file:
         grant codeBase "file:/platform-migratable-api/platform-migration" {
             permission java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read";
         }
 }
 ```
 
The first line `0136 - ACCESS CONTROL PERMISSION FAILURE` indicates this was the 136th failure detected and corresponds to an access control permission check. 
Following that, we can see the permission or permissions involved in the failure and the state of the security context. 
The security context is a stack of domains which in OSGi corresponds to bundles. 
The `*` is prefixed in front of any domain that is not currently granted the permission(s). 
The `-->` is used to identify the domain in the context that is about to generate the failure.
The next section provides information about the current stack. it will show the location in the code in between parenthesis and the name of the bundle the code is located in which will also be prefixed with a `*` if it is detected that the bundle is not granted the permission(s).
As in the context above, a line will show `-->` next to culprit which is responsible for the imminent failure.
The stack will also show a break line (` ----------------------------------------------------------`) whenever a `doPrivileged()` block is detected. 
This means that everything after the block is ignored by the security manager.

After having provided information about the current failure, possible options or solutions will be dumped in a similar fashion. 
Additional information will be provided to indicate what needs to be done. 
For example the `Granting permissions to bundles:` section will list a set of bundles that are given the missing permission(s).
The `Extending privileges at:` section is used to indicate lines in the code where one can introduce a `doPrivileged()` block to solve the issue at hand.
The stack that will follow will show how it would appear if the option were implemented. There should no longer be any failure; thus, no `-->` will be seen.

The final section will list a summary of all possible solutions. Usually, these are sorted in order of priority but it is still up to the developer to assess which one is better suited in the current context. In the example above, there were 2 solutions.
This section is the only one that will be printed out on the console when the `-dump` option is not specified.

Here is an example when an acceptable failure is detected:
```
0141 - ACCEPTABLE ACCESS CONTROL PERMISSION FAILURE
===================================================
Acceptable permission:
    REGEX: java\.io\.FilePermission ".*", "read"
Context:
     bundle-0
 --> *org.codice.thirdparty.tika-bundle
Stack:
     at bundle-0(java.security.AccessControlContext:472)
     at bundle-0(java.security.AccessController:884)
     at bundle-0(java.lang.SecurityManager:549)
     at bundle-0(java.lang.SecurityManager:888)
     at bundle-0(java.io.File:814)
 --> at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.NativeFontDirFinder:47)
     at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.FontFileFinder:75)
     at #*org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FileSystemFontProvider:214)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl$DefaultFontProvider:130)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:149)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:413)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:376)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:350)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:146)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:79)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:128)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:93)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.PDDocumentCatalog:109)
     at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:598)
     at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:545)
     at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.text.PDFTextStripper:267)
     at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDF2XHTML:117)
     at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDFParser:171)
     at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
     at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
     at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
     at *org.apache.tika.core(org.apache.tika.parser.AutoDetectParser:143)
     at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:75)
     at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:68)
     at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:229)
     at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:170)
     at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:153)
     at *catalog-core-standardframework(ddf.catalog.impl.operations.MetacardFactory:75)
     at *catalog-core-standardframework(ddf.catalog.impl.operations.OperationsMetacardSupport:156)
     at *catalog-core-standardframework(ddf.catalog.impl.operations.CreateOperations:141)
     at *catalog-core-standardframework(ddf.catalog.impl.CatalogFrameworkImpl:237)
     at *catalog-rest-impl(Proxy657df4cb_72a9_4651_97be_9316fe1dabb9.create(ddf.catalog.content.operation.CreateStorageRequest)+58)
     at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:764)
     at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:727)
     at *catalog-ui-search(Proxye57e7ff3_143d_40a0_beef_f9bad6b72587.addDocument(java.util.List, javax.servlet.http.HttpServletRequest, java.lang.String, java.io.InputStream)+77)
     at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:375)
     at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:150)
     at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication$$Lambda$808.525212850.handle(spark.Request, spark.Response)+6)
     at *catalog-ui-search(spark.RouteImpl$1:61)
     at *catalog-ui-search(spark.http.matching.Routes:61)
     at *catalog-ui-search(spark.http.matching.MatcherFilter:130)
     at *catalog-ui-search(org.codice.ddf.catalog.ui.SparkServlet:146)
     at *javax.servlet-api(javax.servlet.http.HttpServlet:790)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHolder:840)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1772)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:98)
     at *platform-filter-clientinfo(org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter:72)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *platform-filter-response(org.codice.ddf.platform.response.filter.ResponseFilter:96)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *security-filter-authorization(org.codice.ddf.security.filter.authorization.AuthorizationFilter:103)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:271)
     at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$531.1165582791.run()+12)
     at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction, java.security.AccessControlContext)+-1)
     at bundle-0(javax.security.auth.Subject:422)
    ----------------------------------------------------------
     at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:281)
     at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$530.644881876.call()+16)
     at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
     at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
     at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
     at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:267)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:229)
     at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:122)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *security-filter-csrf(org.codice.ddf.security.filter.csrf.CsrfFilter:146)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
     at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.DelegateServletFilter:119)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1759)
     at *org.eclipse.jetty.websocket.server(org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter:205)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1759)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:582)
     at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler:71)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:143)
     at *org.eclipse.jetty.security(org.eclipse.jetty.security.SecurityHandler:548)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:226)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1180)
     at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceContext:296)
     at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:512)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:185)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1112)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:141)
     at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.JettyServerHandlerCollection:80)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.HandlerWrapper:134)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.Server:539)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpChannel:333)
     at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpConnection:251)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.AbstractConnection$ReadCallback:283)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:108)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.ssl.SslConnection:251)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.AbstractConnection$ReadCallback:283)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:108)
     at *org.eclipse.jetty.io(org.eclipse.jetty.io.SelectChannelEndPoint$2:93)
     at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:303)
     at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:148)
     at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:136)
     at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool:671)
     at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool$2:589)
     at bundle-0(java.lang.Thread:748)
```

The differences above lie in the permission which is reported as a matching regular expression from the pre-configured acceptable failure definition and in a `#` character seen in the 8th stack line which indicates the line was matched against the same pre-configured acceptable failure definition. 
Since it is considered an acceptable failure, no solutions section will be printed out.
 
#### -service
Specifies that a breakpoint should be added in Eclipse's Service Registry to detect internal security checks done for given bundles before dispatching service events. 
These failures are analyzed and reported as normal security check failures. This option tends to slow down the system a bit as the debugger is invoked for all checks and not just when a failure is about to be reported.

#### -grant
When specified, the debugger will use the backdoor and a registered ServicePermission service to temporarily grant permissions for detected security failures which after analysis yields a single solution. 
This is only temporary and will not survive a restart of the VM but will prevent any further failures that would otherwise not be if the permission(s) were defined. 
It also tends to slow down the system since the OSGi permission cache ends up being cleared each time.

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
