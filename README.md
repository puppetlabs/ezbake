ezbake
======

A packaging system for trapperkeeper applications.

TODO: best practices and other docs.

## How to Run

```
lein stage
cd target/staging
rake pl:jenkins:uber_build
```
TODO: `stage` command needs to support some CLI args, to allow you to specify
FOSS vs PE and select which project to build.

## EZBake Project Directory Structure

In the root of the `ezbake` repo, there is a directory called `configs`.  Each
directory inside of that directory represents a configuration for building packages
for a single `ezbake` project.  Inside that directory, you must have an ezbake
project config file, and you may also optionally include some other files that
you'd like to include in the packages.  Here are some details:

### EZBake Project Config File

This file must be named `<project-name>.clj`, where `<project-name>` is the
same string that you used for the name of the directory inside of `configs`.

This file is actually a regular leiningen project file.  It must define:

* The groupId/artifactId for the project.  This will usually be something like
  `puppetlabs.packages/<project-name>`.  (It appears just after the `defproject`
  token.)  This will be used to uniquely identify a particular package that
  includes a particular bundle of TK services.
* The version for the project.  This is just a string literal, the same way
  you'd version any leiningen project file.  This is the version number that
  will be used for the RPM/DEB/other packages that are built.
* `:dependencies`: here you just use the normal leiningen dependencies syntax
  to list out all of the TK projects that you'd like to bundle.  You will specify
  a specific version number for each; this does imply that you will need to have
  done a normal leiningen/maven deploy for the project w/the specified version
  number.
* `:uberjar-name`: Whatever you'd like for the name of the final jar file to be.
* `:repositories`: this is not strictly necessary if you are doing an entirely
  OSS release, and all of the dependencies are available on clojars.  If if any
  of the dependencies are internal/closed-source, you'll probably need to add
  our Nexus release/snapshot repos here.
* `:main`: the name of the main namespace, for use when running `java -jar`.  This
  will almost always be `puppetlabs.trapperkeeper.main` (NOTE: this may go away
  at some point if we decide to get rid of the tiny bit of aot that we are still
  doing.)

### Additional Config Files

Inside of your project dir (e.g., `/configs/jvm-puppet`) you may optionally
create a directory called `config`.  Any files/directories therein will be assumed
to be config files that you'd like to include in the packaging.  They'll be deployed
to the confdir that is used by the relevant packaging system; e.g., `/etc/jvm-puppet`.

It is likely that you'll want to put at least two files here:

#### bootstrap.cfg

The upstream TK projects won't have any knowledge about the fact that they may
be bundled with other ezbake projects, so there's no way they could provide
an appropriate `bootstrap.cfg` file.  You'll probably want to drop one here
and define what services you would like to see enabled by default when the
package is installed.  (An installer / puppet module could always be used after
deploying the package to turn some services on/off.)

#### conf.d/webserver.ini

Any files that you put in `conf.d` will be available to the trapperkeeper config
service on startup.  In the most common case, we'll probably be pulling some of
these files from the upstream TK projects, but there are some config files that
are likely to be duplicated, so we'll need an authoritative copy in ezbake.  The
most likely culprit is `webserver.ini`; probably almost every TK project will
have some web interface, and thus will need to configure a web server (interface,
port, SSL config, etc.).  In ezbake we'll probably usually want to control this
centrally for a given project/bundle, rather than trying to merge webserver config
provided by multiple upstream TK projects.

## Upstream TK Project Directory Structure

EZBake will look inside of the JAR files for all of the upstream TK projects that
it is bundling to see if they have extra files that should be included in the
packaging.  The easiest way to get your extra files into your jar is to simply
define them in the `resources` directory of your upstream TK project, but you
can use any other means you prefer to get them into your jar.  Here are the
conventions for what files ezbake will look for:

### Shared Config Files

Any files found in your jar under the path `ext/config/shared` will be treated
as config files that need to ship in the ezbake packages.  These files will be
dropped into the package inside of the `conf.d` directory.  (e.g: `classifier.ini`,
`rbac.ini`, etc.)

### Doc Files

Any files found in the jar under the path `ext/docs` will be treated as user
documentation that should be included in the package.  The directory structure
will be respected; in the package, these files will end up getting deployed to,
e.g., `/usr/share/doc/jvm-puppet-bundle/jvm-puppet/<orig-directory-structure>/README.md`

### EZBake config file

You may include a file called `ezbake.conf` (HOCON/typesafe config format) in the
`ext` directory in your jar.  This file is used to specify things like names of
other packages that your project has a dependency on.  e.g.:

```
ezbake {
   pe {}
   foss {
      redhat-dependencies: [telnet, screen],
      debian-dependencies: [telnet, screen, cowsay],
   }
}
```

## SNAPSHOTS

If your ezbake leiningen project file specifies a version number that ends with
"-SNAPSHOT", the build will be treated as ephemeral by the Puppet Labs packaging
system.  Otherwise it will be treated as a final release, and you will not be
able to do more than one release with the same groupId/artifactId/version number.
(This follows the typical maven conventions.)

## Manifest Data

When ezbake builds packages, it will include a bit of manifest data to indicate
what versions of all of the upstream TK projects were included in the bundle.  This
manifest data will show up in three places:

* In the `description` field of the rpm metadata
* In the `description` field of the deb metadata
* In a file called `ezbake.manifest` that is included in the package (deployed to
  the same directory that the jar file is deployed to.)