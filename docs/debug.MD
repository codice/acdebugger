## Debug Output

Here is an example of debug output:
```
AC Debugger: 0136 - ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ========================================
AC Debugger: Permission:
AC Debugger:      java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
AC Debugger:  Context:
AC Debugger:       bundle-0
AC Debugger:       org.apache.commons.io
AC Debugger:   --> *platform-migration
AC Debugger:  Stack:
AC Debugger:       at bundle-0(java.security.AccessControlContext:472)
AC Debugger:       at bundle-0(java.security.AccessController:884)
AC Debugger:       at bundle-0(java.lang.SecurityManager:549)
AC Debugger:       at bundle-0(java.lang.SecurityManager:888)
AC Debugger:       at bundle-0(java.io.File:844)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
AC Debugger:       at bundle-0(java.io.File:1291)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
AC Debugger:   --> at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:       at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:      ----------------------------------------------------------
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
AC Debugger:       at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
AC Debugger:       at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
AC Debugger:       at bundle-0(java.util.Iterator:116)
AC Debugger:       at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:481)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:471)
AC Debugger:       at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:234)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline:510)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:       at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
AC Debugger:       at bundle-0(java.util.concurrent.FutureTask:266)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
AC Debugger:       at bundle-0(java.lang.Thread:748)
AC Debugger:  
AC Debugger:  OPTION 1
AC Debugger:  --------
AC Debugger:  Permission:
AC Debugger:      java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
AC Debugger:  Granting permission to bundles:
AC Debugger:      platform-migration
AC Debugger:  Extending privileges at:
AC Debugger:      platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:  Context:
AC Debugger:       bundle-0
AC Debugger:       org.apache.commons.io
AC Debugger:       platform-migration
AC Debugger:  Stack:
AC Debugger:       at bundle-0(java.security.AccessControlContext:472)
AC Debugger:       at bundle-0(java.security.AccessController:884)
AC Debugger:       at bundle-0(java.lang.SecurityManager:549)
AC Debugger:       at bundle-0(java.lang.SecurityManager:888)
AC Debugger:       at bundle-0(java.io.File:844)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
AC Debugger:       at bundle-0(java.io.File:1291)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction))
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:      ----------------------------------------------------------
AC Debugger:       at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
AC Debugger:       at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
AC Debugger:       at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
AC Debugger:       at bundle-0(java.util.Iterator:116)
AC Debugger:       at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:481)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:471)
AC Debugger:       at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:234)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline:510)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
AC Debugger:       at bundle-0(java.util.concurrent.FutureTask:266)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
AC Debugger:       at bundle-0(java.lang.Thread:748)
AC Debugger: 
AC Debugger:  OPTION 2
AC Debugger:  --------
AC Debugger:  Permission:
AC Debugger:      java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read"
AC Debugger:  Granting permission to bundles:
AC Debugger:      platform-migration
AC Debugger:      platform-migratable-api
AC Debugger:  Context:
AC Debugger:       bundle-0
AC Debugger:       org.apache.commons.io
AC Debugger:       platform-migration
AC Debugger:       platform-migratable-api
AC Debugger:  Stack:
AC Debugger:       at bundle-0(java.security.AccessControlContext:472)
AC Debugger:       at bundle-0(java.security.AccessController:884)
AC Debugger:       at bundle-0(java.lang.SecurityManager:549)
AC Debugger:       at bundle-0(java.lang.SecurityManager:888)
AC Debugger:       at bundle-0(java.io.File:844)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123)
AC Debugger:       at bundle-0(java.io.File:1291)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:473)
AC Debugger:       at org.apache.commons.io(org.apache.commons.io.FileUtils:524)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:       at platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$462.135345875.get()+8)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:      ----------------------------------------------------------
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208)
AC Debugger:       at platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207)
AC Debugger:       at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$444.842134888.apply(java.lang.Object)+4)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline$3$1:193)
AC Debugger:       at bundle-0(java.util.Iterator:116)
AC Debugger:       at bundle-0(java.util.Spliterators$IteratorSpliterator:1801)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:481)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:471)
AC Debugger:       at bundle-0(java.util.stream.ReduceOps$ReduceOp:708)
AC Debugger:       at bundle-0(java.util.stream.AbstractPipeline:234)
AC Debugger:       at bundle-0(java.util.stream.ReferencePipeline:510)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:226)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:276)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$408.1094074659.get()+12)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56)
AC Debugger:       at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52)
AC Debugger:       at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:160)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$376.1915231195.call()+4)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
AC Debugger:       at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182)
AC Debugger:       at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
AC Debugger:       at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
AC Debugger:       at bundle-0(java.util.concurrent.FutureTask:266)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
AC Debugger:       at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
AC Debugger:       at bundle-0(java.lang.Thread:748)
AC Debugger:  
AC Debugger:  SOLUTIONS
AC Debugger:  ---------
AC Debugger:  {
AC Debugger:      Add the following permission to the appropriate policy file:
AC Debugger:          grant codeBase "file:/platform-migration" {
AC Debugger:              permission java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read";
AC Debugger:          }
AC Debugger:      and add an AccessController.doPrivileged() block around:
AC Debugger:          platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145)
AC Debugger:  }
AC Debugger:  {
AC Debugger:      Add the following permission to the appropriate policy file:
AC Debugger:          grant codeBase "file:/platform-migratable-api/platform-migration" {
AC Debugger:              permission java.io.FilePermission "/projects/ddf-2.14.0-SNAPSHOT/security/default.policy", "read";
AC Debugger:          }
AC Debugger:  }
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

After having provided information about the current failure, possible options or solutions will be printed out in a similar fashion. 
Additional information will be provided to indicate what needs to be done. 
For example the `Granting permissions to bundles:` section will list a set of bundles that are given the missing permission(s).
The `Extending privileges at:` section is used to indicate lines in the code where one can introduce a `doPrivileged()` block to solve the issue at hand.
The stack that will follow will show how it would appear if the option were implemented. There should no longer be any failure; thus, no `-->` will be seen.

The final section will list a summary of all possible solutions. Usually, these are sorted in order of priority but it is still up to the developer to assess which one is better suited in the current context. In the example above, there were 2 solutions.
This section is the only one that will be printed out on the console when the `--debug` option is not specified.

Here is an example when an acceptable failure is detected:
```
AC Debugger: 0141 - ACCEPTABLE ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ===================================================
AC Debugger: Acceptable permission:
AC Debugger:     REGEX: java\.io\.FilePermission ".*", "read"
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:  --> *org.codice.thirdparty.tika-bundle
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472)
AC Debugger:      at bundle-0(java.security.AccessController:884)
AC Debugger:      at bundle-0(java.lang.SecurityManager:549)
AC Debugger:      at bundle-0(java.lang.SecurityManager:888)
AC Debugger:      at bundle-0(java.io.File:814)
AC Debugger:  --> at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.NativeFontDirFinder:47)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.FontFileFinder:75)
AC Debugger:      at #*org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FileSystemFontProvider:214)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl$DefaultFontProvider:130)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:149)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:413)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:376)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:350)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:146)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:79)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:128)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:93)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.PDDocumentCatalog:109)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:598)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:545)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.text.PDFTextStripper:267)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDF2XHTML:117)
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDFParser:171)
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280)
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.AutoDetectParser:143)
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:75)
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:68)
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:229)
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:170)
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:153)
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.MetacardFactory:75)
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.OperationsMetacardSupport:156)
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.CreateOperations:141)
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.CatalogFrameworkImpl:237)
AC Debugger:      at *catalog-rest-impl(Proxy657df4cb_72a9_4651_97be_9316fe1dabb9.create(ddf.catalog.content.operation.CreateStorageRequest)+58)
AC Debugger:      at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:764)
AC Debugger:      at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:727)
AC Debugger:      at *catalog-ui-search(Proxye57e7ff3_143d_40a0_beef_f9bad6b72587.addDocument(java.util.List, javax.servlet.http.HttpServletRequest, java.lang.String, java.io.InputStream)+77)
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:375)
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:150)
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication$$Lambda$808.525212850.handle(spark.Request, spark.Response)+6)
AC Debugger:      at *catalog-ui-search(spark.RouteImpl$1:61)
AC Debugger:      at *catalog-ui-search(spark.http.matching.Routes:61)
AC Debugger:      at *catalog-ui-search(spark.http.matching.MatcherFilter:130)
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.SparkServlet:146)
AC Debugger:      at *javax.servlet-api(javax.servlet.http.HttpServlet:790)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHolder:840)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1772)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:98)
AC Debugger:      at *platform-filter-clientinfo(org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter:72)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *platform-filter-response(org.codice.ddf.platform.response.filter.ResponseFilter:96)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *security-filter-authorization(org.codice.ddf.security.filter.authorization.AuthorizationFilter:103)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:271)
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$531.1165582791.run()+12)
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction, java.security.AccessControlContext)+-1)
AC Debugger:      at bundle-0(javax.security.auth.Subject:422)
AC Debugger:     ----------------------------------------------------------
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:281)
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$530.644881876.call()+16)
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:267)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:229)
AC Debugger:      at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:122)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *security-filter-csrf(org.codice.ddf.security.filter.csrf.CsrfFilter:146)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91)
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.DelegateServletFilter:119)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1759)
AC Debugger:      at *org.eclipse.jetty.websocket.server(org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter:205)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1759)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:582)
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler:71)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:143)
AC Debugger:      at *org.eclipse.jetty.security(org.eclipse.jetty.security.SecurityHandler:548)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:226)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1180)
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceContext:296)
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:512)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:185)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1112)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:141)
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.JettyServerHandlerCollection:80)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.HandlerWrapper:134)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.Server:539)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpChannel:333)
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpConnection:251)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.AbstractConnection$ReadCallback:283)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:108)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.ssl.SslConnection:251)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.AbstractConnection$ReadCallback:283)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:108)
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.SelectChannelEndPoint$2:93)
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:303)
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:148)
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume:136)
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool:671)
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool$2:589)
AC Debugger:      at bundle-0(java.lang.Thread:748)
```

The differences above lie in the permission which is reported as a matching regular expression from the pre-configured acceptable failure definition and in a `#` character seen in the 8th stack line which indicates the line was matched against the same pre-configured acceptable failure definition. 
Since it is considered an acceptable failure, no solutions section will be printed out.
 
Here is another example illustration a scenario where a combined access control context is in play within the security manager:
```
AC Debugger: 0001 - ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ========================================
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${/}ingest${/}metacard.xml", "read"
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      catalog-core-commands
AC Debugger:      org.apache.shiro.core
AC Debugger:      org.apache.karaf.shell.core
AC Debugger:  --> *org.apache.karaf.shell.ssh (combined)
AC Debugger:      *org.apache.sshd.core (combined)
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472)
AC Debugger:      at bundle-0(java.security.AccessController:884)
AC Debugger:      at bundle-0(java.lang.SecurityManager:549)
AC Debugger:      at bundle-0(java.lang.SecurityManager:888)
AC Debugger:      at bundle-0(java.io.File:814)
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:353)
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:249)
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands$$Lambda$980.479024148.call()+4)
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90)
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83)
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:383)
AC Debugger:      at catalog-core-commands(org.codice.ddf.security.common.Security:182)
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands:88)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229)
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59)
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266)
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142)
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617)
AC Debugger:      at bundle-0(java.lang.Thread:748)
```

In such scenario, the access control context used by the security manager to verify permissions is composed of two parts: the first (as seen in other examples) comes from the current stack and the second part is inherited via a combined access control context. 
Domains that are inherited are listed with **(combined)** next to them. 

So in the above example, the security failure happens not because a class in the stack belongs to a bundle that doesn't have the right permission but because an inherited protection domain is associated with a bundle that doesn't have the permission.
