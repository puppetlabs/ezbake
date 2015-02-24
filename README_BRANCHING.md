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

Similar to most of the other projects at Puppet Labs, there should generally
only be two branches that are relevant at all in the puppetlabs github repo:
`master`, and `stable`.

In the case of EZBake, the `stable` branch currently represents the 0.2.x
series of EZBake plugin releases.  In places where it makes assumptions about
filesystem paths, it uses the *Puppet 3.x* paths.

The `master` branch currently represents the 0.3.x series of EZBake plugin
releases.  In places where it makes assumptions about filesystem paths,
it uses the *Puppet 4.x/AIO* paths.
