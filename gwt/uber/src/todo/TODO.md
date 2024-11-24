TODO: Modernize the xapi-meta plugin to use the new indexed schema data.

For migration, we are simply disabling the creation of paths.xapi file,  
which are needed for GwtcJob to be able to function correctly.

Rather than hack the almost-migrated limbo-version to do this,  
I am simply removing the plugin where it is used,  
and saving the previously-built paths.xapi files,  
so equivalent-or-better files can be built post-migration.

So far, the plan I have is to create a modern XapiManifestTask,  
which will be `tasks.register()`d when each fully indexed module is in scope.  
This way, there will be no / minimal runtime searching to do, and no extra plugin to apply.

For now though, it gets disabled. The migration will break gwtc jobs,  
at least until we finish migrating and then make a rock solid "gwt takes source jars" mechanism,  
likely using paths.xapi, but improved to not have absolute file paths, so they can be checked in.
