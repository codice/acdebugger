## Debug Output

Here is an example of debug output:
```
AC Debugger: 0136 - ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ========================================
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${ddf.home.perm}security${/}default.policy", "read"
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      org.apache.commons.io
AC Debugger:  --> *platform-migration
AC Debugger:      *platform-migratable-api
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=16228)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.io.File:844) <instance of java.io.File(id=16232)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71) <instance of org.apache.commons.io.filefilter.DirectoryFileFilter(id=11809)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56) <instance of org.apache.commons.io.filefilter.NotFileFilter(id=16233)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128) <instance of org.apache.commons.io.filefilter.AndFileFilter(id=16234)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123) <instance of org.apache.commons.io.filefilter.OrFileFilter(id=16235)>
AC Debugger:      at bundle-0(java.io.File:1291) <instance of java.io.File(id=16236)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:473) <class of org.apache.commons.io.FileUtils>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:524) <class of org.apache.commons.io.FileUtils>
AC Debugger:  --> at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346.get()+8) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346(id=16238)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=16239)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:     ----------------------------------------------------------
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104) <instance of org.codice.ddf.security.migratable.impl.SecurityMigratable(id=16158)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955.apply(java.lang.Object)+4) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955(id=11569)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline$3$1:193) <instance of java.util.stream.ReferencePipeline$3$1(id=11570)>
AC Debugger:      at bundle-0(java.util.Iterator:116) <instance of java.util.LinkedHashMap$LinkedValueIterator(id=11571)>
AC Debugger:      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801) <instance of java.util.Spliterators$IteratorSpliterator(id=11573)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:481) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:471) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708) <instance of java.util.stream.ReduceOps$4(id=11577)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:234) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline:510) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl(id=11205)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:228) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:278) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107.get()+12) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107(id=11134)>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=11135)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:      at *platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485.call()+4) <instance of org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485(id=11139)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=11143)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182) <instance of org.codice.ddf.security.common.Security(id=11144)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84) <instance of org.apache.karaf.shell.impl.action.command.ActionCommand(id=11145)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266) <instance of java.util.concurrent.FutureTask(id=11152)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142) <instance of java.util.concurrent.ThreadPoolExecutor(id=11154)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617) <instance of java.util.concurrent.ThreadPoolExecutor$Worker(id=11156)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='pipe-migration:export', id=8571)>
AC Debugger: 
AC Debugger: OPTION 1
AC Debugger: --------
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${ddf.home.perm}security${/}default.policy", "read"
AC Debugger: Granting permission to bundle:
AC Debugger:     platform-migration
AC Debugger: Extending privileges at:
AC Debugger:     platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      org.apache.commons.io
AC Debugger:      platform-migration
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=16228)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.io.File:844) <instance of java.io.File(id=16232)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71) <instance of org.apache.commons.io.filefilter.DirectoryFileFilter(id=11809)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56) <instance of org.apache.commons.io.filefilter.NotFileFilter(id=16233)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128) <instance of org.apache.commons.io.filefilter.AndFileFilter(id=16234)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123) <instance of org.apache.commons.io.filefilter.OrFileFilter(id=16235)>
AC Debugger:      at bundle-0(java.io.File:1291) <instance of java.io.File(id=16236)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:473) <class of org.apache.commons.io.FileUtils>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:524) <class of org.apache.commons.io.FileUtils>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)) <class of java.security.AccessController>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:     ----------------------------------------------------------
AC Debugger:      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346.get()+8) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346(id=16238)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=16239)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104) <instance of org.codice.ddf.security.migratable.impl.SecurityMigratable(id=16158)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955.apply(java.lang.Object)+4) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955(id=11569)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline$3$1:193) <instance of java.util.stream.ReferencePipeline$3$1(id=11570)>
AC Debugger:      at bundle-0(java.util.Iterator:116) <instance of java.util.LinkedHashMap$LinkedValueIterator(id=11571)>
AC Debugger:      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801) <instance of java.util.Spliterators$IteratorSpliterator(id=11573)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:481) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:471) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708) <instance of java.util.stream.ReduceOps$4(id=11577)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:234) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline:510) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl(id=11205)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:228) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:278) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107.get()+12) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107(id=11134)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=11135)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485.call()+4) <instance of org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485(id=11139)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=11143)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182) <instance of org.codice.ddf.security.common.Security(id=11144)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84) <instance of org.apache.karaf.shell.impl.action.command.ActionCommand(id=11145)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266) <instance of java.util.concurrent.FutureTask(id=11152)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142) <instance of java.util.concurrent.ThreadPoolExecutor(id=11154)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617) <instance of java.util.concurrent.ThreadPoolExecutor$Worker(id=11156)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='pipe-migration:export', id=8571)>
AC Debugger: 
AC Debugger: OPTION 2
AC Debugger: --------
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${ddf.home.perm}security${/}default.policy", "read"
AC Debugger: Granting permission to bundles:
AC Debugger:     platform-migration
AC Debugger:     platform-migratable-api
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      org.apache.commons.io
AC Debugger:      platform-migration
AC Debugger:      platform-migratable-api
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=16228)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=11120)>
AC Debugger:      at bundle-0(java.io.File:844) <instance of java.io.File(id=16232)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.DirectoryFileFilter:71) <instance of org.apache.commons.io.filefilter.DirectoryFileFilter(id=11809)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.NotFileFilter:56) <instance of org.apache.commons.io.filefilter.NotFileFilter(id=16233)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.AndFileFilter:128) <instance of org.apache.commons.io.filefilter.AndFileFilter(id=16234)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.filefilter.OrFileFilter:123) <instance of org.apache.commons.io.filefilter.OrFileFilter(id=16235)>
AC Debugger:      at bundle-0(java.io.File:1291) <instance of java.io.File(id=16236)>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:473) <class of org.apache.commons.io.FileUtils>
AC Debugger:      at org.apache.commons.io(org.apache.commons.io.FileUtils:524) <class of org.apache.commons.io.FileUtils>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at platform-migratable-api(org.codice.ddf.migration.ExportMigrationContext:169) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:392) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:362) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346.get()+8) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl$$Lambda$577.1256736346(id=16238)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=16239)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:     ----------------------------------------------------------
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:358) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationEntryImpl:208) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at platform-migratable-api(org.codice.ddf.migration.ExportMigrationEntry:207) <instance of org.codice.ddf.configuration.migration.ExportMigrationEntryImpl(id=16237)>
AC Debugger:      at *security-migratable(org.codice.ddf.security.migratable.impl.SecurityMigratable:104) <instance of org.codice.ddf.security.migratable.impl.SecurityMigratable(id=16158)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:195) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955.apply(java.lang.Object)+4) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl$$Lambda$557.633171955(id=11569)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline$3$1:193) <instance of java.util.stream.ReferencePipeline$3$1(id=11570)>
AC Debugger:      at bundle-0(java.util.Iterator:116) <instance of java.util.LinkedHashMap$LinkedValueIterator(id=11571)>
AC Debugger:      at bundle-0(java.util.Spliterators$IteratorSpliterator:1801) <instance of java.util.Spliterators$IteratorSpliterator(id=11573)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:481) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:471) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReduceOps$ReduceOp:708) <instance of java.util.stream.ReduceOps$4(id=11577)>
AC Debugger:      at bundle-0(java.util.stream.AbstractPipeline:234) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at bundle-0(java.util.stream.ReferencePipeline:510) <instance of java.util.stream.ReferencePipeline$3(id=11576)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ExportMigrationManagerImpl:166) <instance of org.codice.ddf.configuration.migration.ExportMigrationManagerImpl(id=11205)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:228) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:278) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107.get()+12) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager$$Lambda$514.359595107(id=11134)>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils$1:56) <instance of org.codice.ddf.configuration.migration.AccessUtils$1(id=11135)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction)+-1) <class of java.security.AccessController>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.AccessUtils:52) <class of org.codice.ddf.configuration.migration.AccessUtils>
AC Debugger:      at platform-migration(org.codice.ddf.configuration.migration.ConfigurationMigrationManager:162) <instance of org.codice.ddf.configuration.migration.ConfigurationMigrationManager(id=11133)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.ExportCommand:47) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485.call()+4) <instance of org.codice.ddf.migration.commands.MigrationCommand$$Lambda$459.2036866485(id=11139)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=11140)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=11143)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.security.common.Security:182) <instance of org.codice.ddf.security.common.Security(id=11144)>
AC Debugger:      at *admin-core-migration-commands(org.codice.ddf.migration.commands.MigrationCommand:107) <instance of org.codice.ddf.migration.commands.ExportCommand(id=11136)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84) <instance of org.apache.karaf.shell.impl.action.command.ActionCommand(id=11145)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=11148)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386) <instance of org.apache.felix.gogo.runtime.Closure(id=11149)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at *org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59) <instance of org.apache.felix.gogo.runtime.Pipe(id=11150)>
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266) <instance of java.util.concurrent.FutureTask(id=11152)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142) <instance of java.util.concurrent.ThreadPoolExecutor(id=11154)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617) <instance of java.util.concurrent.ThreadPoolExecutor$Worker(id=11156)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='pipe-migration:export', id=8571)>
AC Debugger: 
AC Debugger: SOLUTIONS
AC Debugger: ---------
AC Debugger: {
AC Debugger:     Add the following permission block to the appropriate policy file:
AC Debugger:         grant codeBase "file:/platform-migration" {
AC Debugger:             permission java.io.FilePermission "${ddf.home.perm}security${/}default.policy", "read";
AC Debugger:         }
AC Debugger:     and add an AccessController.doPrivileged() block around:
AC Debugger:         platform-migration(org.codice.ddf.configuration.migration.ExportMigrationContextImpl:145) <instance of org.codice.ddf.configuration.migration.ExportMigrationContextImpl(id=16149)>
AC Debugger: }
AC Debugger: {
AC Debugger:     Add the following permission block to the appropriate policy file:
AC Debugger:         grant codeBase "file:/platform-migratable-api/platform-migration" {
AC Debugger:             permission java.io.FilePermission "${ddf.home.perm}security${/}default.policy", "read";
AC Debugger:         }
AC Debugger: }
```

The first line `0136 - ACCESS CONTROL PERMISSION FAILURE` indicates this was the 136th failure detected and corresponds to an access control permission check. 
Following that, we can see the permission or permissions involved in the failure and the state of the security context. 
The security context is a stack of domains which in OSGi corresponds to bundles and for non-OSGi containers, it corresponds to the domain location. 
The `*` is prefixed in front of any domain that is not currently granted the permission(s). 
The `-->` is used to identify the domain in the context that is about to generate the failure.
The next section provides information about the current stack. 
It will show the location in the code in between parenthesis and the name of the bundle or the domain codesource location where the code is located in which will also be prefixed with a `*` if it is detected that the bundle or domain is not granted the permission(s).
At the end of each line in between `<>` we can find information about the runtime object (e.g. `instance of java.security.AccessControlContext(id=16228)>`) for instance methods or the class (e.g. `<class of java.security.AccessController>`) for static methods. 
As in the context above, a line will show `-->` next to culprit which is responsible for the imminent failure.
The stack will also show a break line (` ----------------------------------------------------------`) whenever a `doPrivileged()` block is detected. 
This means that everything after the block is ignored by the security manager.

After having provided information about the current failure, possible options or solutions will be printed out in a similar fashion. 
Additional information will be provided to indicate what needs to be done. 
For example the `Granting permissions to bundles:` or `Granting permissions to domains:` section will list a set of bundles or domains that are given the missing permission(s).
The `Extending privileges at:` section is used to indicate lines in the code where one can introduce a `doPrivileged()` block to solve the issue at hand.
The stack that will follow will show how it would appear if the option were implemented. There should no longer be any failure; thus, no `-->` will be seen.

The final section will list a summary of all possible solutions. Usually, these are sorted in order of priority but it is still up to the developer to assess which one is better suited in the current context. In the example above, there were 2 solutions.
This section is the only one that will be printed out on the console when the `--debug` option is not specified.

Here is an example when an acceptable failure is detected:
```
AC Debugger: 0141 - ACCEPTABLE ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ===================================================
AC Debugger: Acceptable permissions:
AC Debugger:     REGEX: java\.io\.FilePermission ".*", "read"
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:  --> *org.codice.thirdparty.tika-bundle
AC Debugger:      *org.apache.tika.core
AC Debugger:      *catalog-transformer-pdf
AC Debugger:      *catalog-core-standardframework
AC Debugger:      *catalog-rest-impl
AC Debugger:      *catalog-ui-search
AC Debugger:      *javax.servlet-api
AC Debugger:      *org.eclipse.jetty.servlet
AC Debugger:      *platform-filter-delegate
AC Debugger:      *platform-filter-clientinfo
AC Debugger:      *platform-filter-response
AC Debugger:      *security-filter-authorization
AC Debugger:      *security-filter-login
AC Debugger:      *org.apache.cxf.cxf-rt-transports-http (combined)
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=1724)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.io.File:814) <instance of java.io.File(id=2088)>
AC Debugger:  --> at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.NativeFontDirFinder:47) <instance of org.apache.fontbox.util.autodetect.MacFontDirFinder(id=2089)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.fontbox.util.autodetect.FontFileFinder:75) <instance of org.apache.fontbox.util.autodetect.FontFileFinder(id=2093)>
AC Debugger:      at #*org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FileSystemFontProvider:214) <instance of org.apache.pdfbox.pdmodel.font.FileSystemFontProvider(id=2094)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl$DefaultFontProvider:130) <class of org.apache.pdfbox.pdmodel.font.FontMapperImpl$DefaultFontProvider>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:149) <instance of org.apache.pdfbox.pdmodel.font.FontMapperImpl(id=2095)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:413) <instance of org.apache.pdfbox.pdmodel.font.FontMapperImpl(id=2095)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:376) <instance of org.apache.pdfbox.pdmodel.font.FontMapperImpl(id=2095)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.FontMapperImpl:350) <instance of org.apache.pdfbox.pdmodel.font.FontMapperImpl(id=2095)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:146) <instance of org.apache.pdfbox.pdmodel.font.PDType1Font(id=2096)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.font.PDType1Font:79) <class of org.apache.pdfbox.pdmodel.font.PDType1Font>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:128) <instance of org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm(id=2097)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm:93) <instance of org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm(id=2097)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.pdmodel.PDDocumentCatalog:109) <instance of org.apache.pdfbox.pdmodel.PDDocumentCatalog(id=2098)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:598) <instance of org.apache.tika.parser.pdf.PDF2XHTML(id=2099)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.AbstractPDF2XHTML:545) <instance of org.apache.tika.parser.pdf.PDF2XHTML(id=2099)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.pdfbox.text.PDFTextStripper:267) <instance of org.apache.tika.parser.pdf.PDF2XHTML(id=2099)>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDF2XHTML:117) <class of org.apache.tika.parser.pdf.PDF2XHTML>
AC Debugger:      at *org.codice.thirdparty.tika-bundle(org.apache.tika.parser.pdf.PDFParser:171) <instance of org.apache.tika.parser.pdf.PDFParser(id=2101)>
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280) <instance of org.apache.tika.parser.DefaultParser(id=2103)>
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280) <instance of org.apache.tika.parser.DefaultParser(id=2106)>
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.CompositeParser:280) <instance of org.apache.tika.parser.AutoDetectParser(id=2107)>
AC Debugger:      at *org.apache.tika.core(org.apache.tika.parser.AutoDetectParser:143) <instance of org.apache.tika.parser.AutoDetectParser(id=2107)>
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:75) <instance of ddf.catalog.transformer.common.tika.TikaMetadataExtractor(id=2109)>
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.common.tika.TikaMetadataExtractor:68) <instance of ddf.catalog.transformer.common.tika.TikaMetadataExtractor(id=2109)>
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:229) <instance of ddf.catalog.transformer.input.pdf.PdfInputTransformer(id=2112)>
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:170) <instance of ddf.catalog.transformer.input.pdf.PdfInputTransformer(id=2112)>
AC Debugger:      at *catalog-transformer-pdf(ddf.catalog.transformer.input.pdf.PdfInputTransformer:153) <instance of ddf.catalog.transformer.input.pdf.PdfInputTransformer(id=2112)>
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.MetacardFactory:75) <instance of ddf.catalog.impl.operations.MetacardFactory(id=2114)>
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.OperationsMetacardSupport:156) <instance of ddf.catalog.impl.operations.OperationsMetacardSupport(id=2117)>
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.operations.CreateOperations:141) <instance of ddf.catalog.impl.operations.CreateOperations(id=2119)>
AC Debugger:      at *catalog-core-standardframework(ddf.catalog.impl.CatalogFrameworkImpl:237) <instance of ddf.catalog.impl.CatalogFrameworkImpl(id=2121)>
AC Debugger:      at *catalog-rest-impl(Proxy905e4d32_0fbc_4807_b484_89b3e6deb928.create(ddf.catalog.content.operation.CreateStorageRequest)+58) <instance of Proxy905e4d32_0fbc_4807_b484_89b3e6deb928(id=2123)>
AC Debugger:      at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:764) <instance of org.codice.ddf.rest.impl.CatalogServiceImpl(id=2127)>
AC Debugger:      at *catalog-rest-impl(org.codice.ddf.rest.impl.CatalogServiceImpl:727) <instance of org.codice.ddf.rest.impl.CatalogServiceImpl(id=2127)>
AC Debugger:      at *catalog-ui-search(Proxyf5cc0239_2d9c_43fe_ac67_d5c072bebf08.addDocument(java.util.List, javax.servlet.http.HttpServletRequest, java.lang.String, java.io.InputStream)+77) <instance of Proxyf5cc0239_2d9c_43fe_ac67_d5c072bebf08(id=2130)>
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:375) <instance of org.codice.ddf.catalog.ui.catalog.CatalogApplication(id=2133)>
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication:150) <instance of org.codice.ddf.catalog.ui.catalog.CatalogApplication(id=2133)>
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.catalog.CatalogApplication$$Lambda$1008.951120665.handle(spark.Request, spark.Response)+6) <instance of org.codice.ddf.catalog.ui.catalog.CatalogApplication$$Lambda$1008.951120665(id=2136)>
AC Debugger:      at *catalog-ui-search(spark.RouteImpl$1:61) <instance of spark.RouteImpl$1(id=2138)>
AC Debugger:      at *catalog-ui-search(spark.http.matching.Routes:61) <class of spark.http.matching.Routes>
AC Debugger:      at *catalog-ui-search(spark.http.matching.MatcherFilter:130) <instance of spark.http.matching.MatcherFilter(id=2140)>
AC Debugger:      at *catalog-ui-search(org.codice.ddf.catalog.ui.SparkServlet:146) <instance of org.codice.ddf.catalog.ui.SparkServlet(id=2142)>
AC Debugger:      at *javax.servlet-api(javax.servlet.http.HttpServlet:790) <instance of org.codice.ddf.catalog.ui.SparkServlet(id=2142)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHolder:857) <instance of org.eclipse.jetty.servlet.ServletHolder(id=2146)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1655) <instance of org.eclipse.jetty.servlet.ServletHandler$CachedChain(id=2149)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:98) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *platform-filter-clientinfo(org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter:72) <instance of org.codice.ddf.platform.filter.clientinfo.ClientInfoFilter(id=2154)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *platform-filter-response(org.codice.ddf.platform.response.filter.ResponseFilter:96) <instance of org.codice.ddf.platform.response.filter.ResponseFilter(id=2157)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *security-filter-authorization(org.codice.ddf.security.filter.authorization.AuthorizationFilter:103) <instance of org.codice.ddf.security.filter.authorization.AuthorizationFilter(id=2160)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:271) <class of org.codice.ddf.security.filter.login.LoginFilter>
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$642.1222613506.run()+12) <instance of org.codice.ddf.security.filter.login.LoginFilter$$Lambda$642.1222613506(id=2165)>
AC Debugger:      at bundle-0(java.security.AccessController.doPrivileged(java.security.PrivilegedExceptionAction, java.security.AccessControlContext)+-1) <class of java.security.AccessController>
AC Debugger:      at bundle-0(javax.security.auth.Subject:422) <class of javax.security.auth.Subject>
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:281) <class of org.codice.ddf.security.filter.login.LoginFilter>
AC Debugger:     ----------------------------------------------------------
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter$$Lambda$641.490890921.call()+16) <instance of org.codice.ddf.security.filter.login.LoginFilter$$Lambda$641.490890921(id=2168)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=2170)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=2170)>
AC Debugger:      at *org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=2174)>
AC Debugger:      at *security-filter-login(org.codice.ddf.security.filter.login.LoginFilter:267) <instance of org.codice.ddf.security.filter.login.LoginFilter(id=2176)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:229) <instance of org.codice.ddf.security.filter.websso.WebSSOFilter(id=2178)>
AC Debugger:      at *security-filter-web-sso(org.codice.ddf.security.filter.websso.WebSSOFilter:122) <instance of org.codice.ddf.security.filter.websso.WebSSOFilter(id=2178)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *security-filter-csrf(org.codice.ddf.security.filter.csrf.CsrfFilter:144) <instance of org.codice.ddf.security.filter.csrf.CsrfFilter(id=2182)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.ProxyFilterChain:91) <instance of org.codice.ddf.platform.filter.delegate.ProxyFilterChain(id=2151)>
AC Debugger:      at *platform-filter-delegate(org.codice.ddf.platform.filter.delegate.DelegateServletFilter:119) <instance of org.codice.ddf.platform.filter.delegate.DelegateServletFilter(id=2186)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1642) <instance of org.eclipse.jetty.servlet.ServletHandler$CachedChain(id=2187)>
AC Debugger:      at *org.eclipse.jetty.websocket.server(org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter:215) <instance of org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter(id=2189)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler$CachedChain:1642) <instance of org.eclipse.jetty.servlet.ServletHandler$CachedChain(id=2192)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:533) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler(id=2194)>
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler:71) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler(id=2194)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:146) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler(id=2194)>
AC Debugger:      at *org.eclipse.jetty.security(org.eclipse.jetty.security.SecurityHandler:548) <instance of org.eclipse.jetty.security.ConstraintSecurityHandler(id=2202)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.HandlerWrapper:132) <instance of org.eclipse.jetty.server.session.SessionHandler(id=2207)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:257) <instance of org.eclipse.jetty.server.session.SessionHandler(id=2207)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:1595) <instance of org.eclipse.jetty.server.session.SessionHandler(id=2207)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:255) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1317) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.HttpServiceContext:293) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:203) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler(id=2194)>
AC Debugger:      at *org.eclipse.jetty.servlet(org.eclipse.jetty.servlet.ServletHandler:473) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceServletHandler(id=2194)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.session.SessionHandler:1564) <instance of org.eclipse.jetty.server.session.SessionHandler(id=2207)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:201) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ContextHandler:1219) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.ScopedHandler:144) <instance of org.ops4j.pax.web.service.jetty.internal.HttpServiceContext(id=2209)>
AC Debugger:      at *org.ops4j.pax.web.pax-web-jetty(org.ops4j.pax.web.service.jetty.internal.JettyServerHandlerCollection:80) <instance of org.ops4j.pax.web.service.jetty.internal.JettyServerHandlerCollection(id=2213)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.handler.HandlerWrapper:132) <instance of org.ops4j.pax.web.service.jetty.internal.JettyServerWrapper(id=2214)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.Server:531) <instance of org.ops4j.pax.web.service.jetty.internal.JettyServerWrapper(id=2214)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpChannel:352) <instance of org.eclipse.jetty.server.HttpChannelOverHttp(id=2218)>
AC Debugger:      at *org.eclipse.jetty.server(org.eclipse.jetty.server.HttpConnection:260) <instance of org.eclipse.jetty.server.HttpConnection(id=2221)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.AbstractConnection$ReadCallback:281) <instance of org.eclipse.jetty.io.AbstractConnection$ReadCallback(id=2223)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:102) <instance of org.eclipse.jetty.io.AbstractEndPoint$1(id=2227)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.ssl.SslConnection:291) <instance of org.eclipse.jetty.io.ssl.SslConnection(id=2230)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.ssl.SslConnection$3:151) <instance of org.eclipse.jetty.io.ssl.SslConnection$3(id=2232)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.FillInterest:102) <instance of org.eclipse.jetty.io.AbstractEndPoint$1(id=2233)>
AC Debugger:      at *org.eclipse.jetty.io(org.eclipse.jetty.io.ChannelEndPoint$2:118) <instance of org.eclipse.jetty.io.ChannelEndPoint$2(id=2235)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.EatWhatYouKill:333) <instance of org.eclipse.jetty.util.thread.strategy.EatWhatYouKill(id=2237)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.EatWhatYouKill:310) <instance of org.eclipse.jetty.util.thread.strategy.EatWhatYouKill(id=2237)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.EatWhatYouKill:168) <instance of org.eclipse.jetty.util.thread.strategy.EatWhatYouKill(id=2237)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.strategy.EatWhatYouKill:126) <instance of org.eclipse.jetty.util.thread.strategy.EatWhatYouKill(id=2237)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread:366) <instance of org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread(id=2241)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool:762) <instance of org.eclipse.jetty.util.thread.QueuedThreadPool(id=2243)>
AC Debugger:      at *org.eclipse.jetty.util(org.eclipse.jetty.util.thread.QueuedThreadPool$2:680) <instance of org.eclipse.jetty.util.thread.QueuedThreadPool$2(id=2245)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='qtp2078420815-91', id=58)>
```

The differences above lie in the permission which is reported as a matching regular expression from the pre-configured acceptable failure definition and in a `#` character seen in the 8th stack line which indicates the line was matched against the same pre-configured acceptable failure definition. 
Since it is considered an acceptable failure, no solutions section will be printed out.
 
Here is another example illustrating a scenario where a combined access control context is in play within the security manager:
```
AC Debugger: 0007 - ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ========================================
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${/}metacard.xml", "read"
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      catalog-core-commands
AC Debugger:      org.apache.shiro.core
AC Debugger:      org.apache.karaf.shell.core
AC Debugger:  --> *org.apache.karaf.shell.ssh (combined)
AC Debugger:      *org.apache.sshd.core (combined)
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=3225)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.io.File:814) <instance of java.io.File(id=3234)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:353) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:249) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands$$Lambda$1405.1219594442.call()+4) <instance of org.codice.ddf.commands.catalog.SubjectCommands$$Lambda$1405.1219594442(id=3235)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=3236)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=3236)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=3237)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.security.common.Security:182) <instance of org.codice.ddf.security.common.Security(id=2978)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands:88) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84) <instance of org.apache.karaf.shell.impl.action.command.ActionCommand(id=2984)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=2987)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=2987)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266) <instance of java.util.concurrent.FutureTask(id=2993)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142) <instance of java.util.concurrent.ThreadPoolExecutor(id=2995)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617) <instance of java.util.concurrent.ThreadPoolExecutor$Worker(id=2997)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='pipe-catalog:ingest /metacard.xml', id=2892)>
AC Debugger: 
AC Debugger: OPTION 1
AC Debugger: --------
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${/}metacard.xml", "read"
AC Debugger: Granting permission to bundles:
AC Debugger:     org.apache.karaf.shell.ssh
AC Debugger:     org.apache.sshd.core
AC Debugger: Context:
AC Debugger:      bundle-0
AC Debugger:      catalog-core-commands
AC Debugger:      org.apache.shiro.core
AC Debugger:      org.apache.karaf.shell.core
AC Debugger:      org.apache.karaf.shell.ssh (combined)
AC Debugger:      org.apache.sshd.core (combined)
AC Debugger: Stack:
AC Debugger:      at bundle-0(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=3225)>
AC Debugger:      at bundle-0(java.security.AccessController:884) <class of java.security.AccessController>
AC Debugger:      at bundle-0(java.lang.SecurityManager:549) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.lang.SecurityManager:888) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=2085)>
AC Debugger:      at bundle-0(java.io.File:814) <instance of java.io.File(id=3234)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:353) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.IngestCommand:249) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands$$Lambda$1405.1219594442.call()+4) <instance of org.codice.ddf.commands.catalog.SubjectCommands$$Lambda$1405.1219594442(id=3235)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:90) <instance of org.apache.shiro.subject.support.SubjectCallable(id=3236)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.SubjectCallable:83) <instance of org.apache.shiro.subject.support.SubjectCallable(id=3236)>
AC Debugger:      at org.apache.shiro.core(org.apache.shiro.subject.support.DelegatingSubject:387) <instance of ddf.security.impl.SubjectImpl(id=3237)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.security.common.Security:182) <instance of org.codice.ddf.security.common.Security(id=2978)>
AC Debugger:      at catalog-core-commands(org.codice.ddf.commands.catalog.SubjectCommands:88) <instance of org.codice.ddf.commands.catalog.IngestCommand(id=2981)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.action.command.ActionCommand:84) <instance of org.apache.karaf.shell.impl.action.command.ActionCommand(id=2984)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:68) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=2987)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand:86) <instance of org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand(id=2987)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:571) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:497) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Closure:386) <instance of org.apache.felix.gogo.runtime.Closure(id=2989)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:417) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:229) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at org.apache.karaf.shell.core(org.apache.felix.gogo.runtime.Pipe:59) <instance of org.apache.felix.gogo.runtime.Pipe(id=2991)>
AC Debugger:      at bundle-0(java.util.concurrent.FutureTask:266) <instance of java.util.concurrent.FutureTask(id=2993)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor:1142) <instance of java.util.concurrent.ThreadPoolExecutor(id=2995)>
AC Debugger:      at bundle-0(java.util.concurrent.ThreadPoolExecutor$Worker:617) <instance of java.util.concurrent.ThreadPoolExecutor$Worker(id=2997)>
AC Debugger:      at bundle-0(java.lang.Thread:748) <instance of java.lang.Thread(name='pipe-catalog:ingest /metacard.xml', id=2892)>
AC Debugger: 
AC Debugger: SOLUTIONS
AC Debugger: ---------
AC Debugger: {
AC Debugger:     Add the following permission block to the appropriate policy file:
AC Debugger:         grant codeBase "file:/org.apache.karaf.shell.ssh/org.apache.sshd.core" {
AC Debugger:             permission java.io.FilePermission "${/}metacard.xml", "read";
AC Debugger:         }
AC Debugger: }
```

In such scenario, the access control context used by the security manager to verify permissions is composed of two parts: the first (as seen in other examples) comes from the current stack and the second part is inherited via a combined access control context. 
Domains that are inherited are listed with **(combined)** next to them. 

So in the above example, the security failure happens not because a class in the stack belongs to a bundle that doesn't have the right permission but because an inherited protection domain is associated with a bundle that doesn't have the permission.

For Non-OSGi containers, the bundle name is simply replaced with the domain's codesource location. Instead of `bundle-0` we can see `boot:` to represent the boot classloader domain.

Here is an example:
```
AC Debugger: 0039 - ACCESS CONTROL PERMISSION FAILURE
AC Debugger: ========================================
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${solr.solr.home}${/}server${/}/lib${/}jts-core-1.15.0.jar", "read"
AC Debugger: Context:
AC Debugger:      boot:
AC Debugger:  --> *file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar
AC Debugger:      *file:${solr.solr.home}${/}server${/}start.jar
AC Debugger: Stack:
AC Debugger:      at boot:(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=799)>
AC Debugger:      at boot:(java.security.AccessController:895) <class of java.security.AccessController>
AC Debugger:  --> at *file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar(java.lang.SecurityManager:322) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=100)>
AC Debugger:      at *file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar(java.lang.SecurityManager:661) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=100)>
AC Debugger:      at boot:(sun.nio.fs.UnixPath:818) <instance of sun.nio.fs.UnixPath(id=803)>
AC Debugger:      at boot:(sun.nio.fs.UnixFileAttributeViews$Basic:49) <instance of sun.nio.fs.UnixFileAttributeViews$Basic(id=804)>
AC Debugger:      at boot:(sun.nio.fs.UnixFileSystemProvider:145) <instance of sun.nio.fs.MacOSXFileSystemProvider(id=123)>
AC Debugger:      at boot:(java.nio.file.Files:1763) <class of java.nio.file.Files>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:219) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:276) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:373) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.Files:2760) <class of java.nio.file.Files>
AC Debugger:      at *file:${solr.solr.home}${/}server/start.jar(org.eclipse.jetty.start.BaseHome:397) <instance of org.eclipse.jetty.start.BaseHome(id=111)>
AC Debugger:      at *file:${solr.solr.home}${/}server/start.jar(org.eclipse.jetty.start.StartArgs:487) <instance of org.eclipse.jetty.start.StartArgs(id=602)>
AC Debugger:      at *file:${solr.solr.home}${/}server/start.jar(org.eclipse.jetty.start.Main:351) <instance of org.eclipse.jetty.start.Main(id=112)>
AC Debugger:      at boot:(org.eclipse.jetty.start.Main:75) <class of org.eclipse.jetty.start.Main>
AC Debugger:
AC Debugger: OPTION 1
AC Debugger: --------
AC Debugger: Permission:
AC Debugger:     java.io.FilePermission "${solr.solr.home}${/}server${/}lib${/}jts-core-1.15.0.jar", "read"
AC Debugger: Granting permission to domains:
AC Debugger:     file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar
AC Debugger:     file:${solr.solr.home}${/}server${/}start.jar
AC Debugger: Context:
AC Debugger:      boot:
AC Debugger:      file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar
AC Debugger:      file:${solr.solr.home}${/}server${/}start.jar
AC Debugger: Stack:
AC Debugger:      at boot:(java.security.AccessControlContext:472) <instance of java.security.AccessControlContext(id=799)>
AC Debugger:      at boot:(java.security.AccessController:895) <class of java.security.AccessController>
AC Debugger:      at file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar(java.lang.SecurityManager:322) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=100)>
AC Debugger:      at file:${solr.solr.home}${/}security${/}pro-grade-1.1.3.jar(java.lang.SecurityManager:661) <instance of net.sourceforge.prograde.sm.ProGradeJSM(id=100)>
AC Debugger:      at boot:(sun.nio.fs.UnixPath:818) <instance of sun.nio.fs.UnixPath(id=803)>
AC Debugger:      at boot:(sun.nio.fs.UnixFileAttributeViews$Basic:49) <instance of sun.nio.fs.UnixFileAttributeViews$Basic(id=804)>
AC Debugger:      at boot:(sun.nio.fs.UnixFileSystemProvider:145) <instance of sun.nio.fs.MacOSXFileSystemProvider(id=123)>
AC Debugger:      at boot:(java.nio.file.Files:1763) <class of java.nio.file.Files>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:219) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:276) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.FileTreeWalker:373) <instance of java.nio.file.FileTreeWalker(id=610)>
AC Debugger:      at boot:(java.nio.file.Files:2760) <class of java.nio.file.Files>
AC Debugger:      at file:${solr.solr.home}${/}server${/}start.jar(org.eclipse.jetty.start.BaseHome:397) <instance of org.eclipse.jetty.start.BaseHome(id=111)>
AC Debugger:      at file:${solr.solr.home}${/}server${/}start.jar(org.eclipse.jetty.start.StartArgs:487) <instance of org.eclipse.jetty.start.StartArgs(id=602)>
AC Debugger:      at file:${solr.solr.home}${/}server${/}start.jar(org.eclipse.jetty.start.Main:351) <instance of org.eclipse.jetty.start.Main(id=112)>
AC Debugger:      at boot:(org.eclipse.jetty.start.Main:75) <class of org.eclipse.jetty.start.Main>
AC Debugger:
AC Debugger: SOLUTIONS
AC Debugger: ---------
AC Debugger: {
AC Debugger:     Add the following permission blocks to the appropriate policy file:
AC Debugger:         grant codeBase "file:${solr.solr.home}${/}security/pro-grade-1.1.3.jar" {
AC Debugger:             permission java.io.FilePermission "${solr.solr.home}${/}server${/}lib${/}jts-core-1.15.0.jar", "read";
AC Debugger:         }
AC Debugger:         grant codeBase "file:${solr.solr.home}${/}server/start.jar" {
AC Debugger:             permission java.io.FilePermission "${solr.solr.home}${/}server${/}lib${/}jts-core-1.15.0.jar", "read";
AC Debugger:         }
AC Debugger: }
```