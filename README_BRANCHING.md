This document attempts to capture the details of our branching strategy
for EZbake.

## NOTE: EZBake is now a lein plugin!

Now that EZBake is a lein plugin, all projects that are
using EZBake for their builds should now be referencing a specific,
fixed version number of the plugin in their project.clj.  (e.g., '0.2.3').
These version numbers reference official, stable, final, immutable, released
versions of EZBake that are deployed to our nexus server.  Therefore,
we no longer need to worry about any correlation between the build process
for individual projects and the branching strategy of EZBake itself.

This document is only relevant for users who need to make changes to EZBake
and release new versions of the lein plugin, which may later be consumed
by other projects by incrementing the plugin version number that is
referenced in their project.clj files.

## EZBake branches

The branching strategy is now covered in the
[Branching Strategy page on the EZBake wiki](https://github.com/puppetlabs/ezbake/wiki/Branching-Strategy).
