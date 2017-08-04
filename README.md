lein-ezbake
======

EZBake is a leiningen plugin that integrates multiple Trapperkeeper services
and their config files into a single uberjar and stages them in preparation for
packaging.

## EZBake branches

The branching strategy is now covered in the
[Branching Strategy page on the EZBake wiki](https://github.com/puppetlabs/ezbake/wiki/Branching-Strategy).

## Minimum Trapperkeeper version dependencies

EZBake 1.0 and later utilize the
![restart-file](https://github.com/puppetlabs/trapperkeeper/blob/1.5.1/documentation/Restart-File.md)
feature in Trapperkeeper to monitor service start and reload status.  For this
reason, the application being packaged must include Trapperkeeper 1.5.1 or
later.  If an earlier version is used instead, the service will fail to be
started properly due to the lack of support for the `-r | --restart-file`
command line option in the earlier Trapperkeeper versions.

The failure message would be written to the
`/var/log/puppetlabs/<app>/<app>-daemon.log` file -- for sysvinit / upstart --
or journal -- for systemd -- with text which looks like this:

```
Error(s) occurred while parsing command-line arguments: Unknown option: "--restart-file"
```

## Using

To get started using EZBake, please add the following to the value for the
`:plugins` key in your `project.clj`:

[![Clojars Project](http://clojars.org/puppetlabs/lein-ezbake/latest-version.svg)](http://clojars.org/puppetlabs/lein-ezbake)

Before you can get started using it, however, there may be some additional
configuration necessary.

### Configuring

```clojure
{:lein-ezbake {

  ; Configures how lein-ezbake manages resources. "Resources" primarily refers
  ; to the templates ezbake uses to build packages.
  :resources {

    ; The resources type indicates where lein-ezbake gets resources from.
    ; Currently only resources stored in the lein-ezbake jar can be used. Future
    ; versions of lein-ezbake may support pulling these resources instead from a
    ; specific version of some git repository.
    :type :jar

    ; This directory refers to the location in the current project where
    ; resources will be dumped.
    :dir "tmp/config"}

  ; This is the directory where lein-ezbake will look for additional
  ; configuration files to copy into the staging directory.
  :config-dir "config"

  ; If specified, this is a directory where lein-ezbake will look for additional
  ; configuration files will end up under the install directory under /opt. It's
  ; intended for configuration files that an end user should not edit
  :system-config-dir "system-config"

  ; These variables are available to either modify the behavior of lein-ezbake
  ; in various ways or to populate values in template files.
  :vars {
    :user "puppet"
    :group "puppet"
    :start-timeout "120"
    :build-type "foss"
    :java-args "-Xms2g -Xmx2g -XX:MaxPermSize=256m"
    :logrotate-enabled true}}}

```

In addition to this standard configuration, there are two primary use cases for
`lein-ezbake` which lead to different configuration parameters. The differences
are considered in further depth under [Standalone Projects](#standalone-projects)
and [Composite Projects](#composite-projects).

#### Standalone vs Composite EZBake Projects

EZBake projects generally fall under two categories based on the situations in
which they are used: standalone and composite.

A Standalone ezbake project is one in which the code for one of the
trapperkeeper services packaged by `lein-ezbake` is included in the same project
repository that is used to define the ezbake configuration values. The
prototypical example of this type of project is [Puppet
Server](https://github.com/puppetlabs/puppet-server).

A Composite ezbake project is one in which the primary purpose of the repository
where the ezbake configuration values are defined is to compose multiple
trapperkeeper services into a single package using ezbake. The prototypical
example of this type of project is
[pe-console-services](https://github.com/puppetlabs/pe-console-services).

#### Standalone

Since a standalone ezbake project is often defined in the same repository as one
of the TK services it is intended to package and therefore shares a
`project.clj`, it may be necessary to overwrite some values used by ezbake when
creating the staging directory.

Specifically, the `:name` and `:dependencies` keys may need to be replaced as in
the [Puppet Server](https://github.com/puppetlabs/puppet-server) example below.
The `:name` replacement is necessary to avoid name collision when an ezbake
project also consists of clojure code that defines its own TK services and the
Maven artifact name does not match the desired package name.

```clojure
{:profiles {
  :ezbake {
    :dependencies ^:replace
      [[puppetlabs/puppet-server "0.4.2-SNAPSHOT"]
       [puppetlabs/trapperkeeper-webserver-jetty9 "0.9.0"]
       [org.clojure/tools.nrepl "0.2.3"]]
    :name "puppetserver"}}}
```

Note that it is necessary here to use the `^:replace` metadata on the
`:dependencies` list since Leiningen's default behavior is to append
dependencies defined in a lein profile.

Note that the symble `:ezbake` is not strictly necessary here.

#### Composite

Composite EZBake projects usually do not define their own services but rather
provide a list of dependencies which themselves define TK services.  Because of
this it is not strictly necessary to define a profile such as `:ezbake` shown
above; although it is conceivable that such a composite project may define its
own services, it is unlikely and ill-advised because no one likes blurred lines
in architectural diagrams. Just look at the Leaning Tower of Pisa.

#### Additional Uberjars

Some projects might have a use case for automatically fetching a versioned
jar from an external repository that needs to be placed into the package and be
available after installation.

The `:additional-uberjars` setting can be set to a list of project coordinates
that will be resolved and have uberjars built from them. For example:

```clojure
{:lein-ezbake {
  :additional-uberjars [[puppetlabs/puppetserver "2.7.2"]]
...}}
```

This would result in a puppetserver uberjar being built, and the jar being placed in the
same directory as your project's own uberjar after installation.

The filename of the built uberjar is determined by what value `:uberjar-name`
is set to in its `project.clj`

EZBake will attempt to resolve the coordinates of these external uberjars using
the repositories specified in the `:repositories` key of your project.clj

#### cli-defaults.sh
`cli-defaults.sh` is intended to be a file from which default shell script
variables can be defined by a project, and be loaded by EZBake's `cli-app`
script. That is, the script which is run when commands like
`service my-project start` are called.

This differs from the `default` files, which also get loaded by `cli-app`, in
that a project has no control over what goes into `default`. `cli-defaults.sh`
is under the control of your project.

For `cli-defaults.sh` to be used, it simply needs to exist at
`resources/ext/cli_defaults/cli-defaults.sh.erb` in your project. Since it is
an erb template, it will have access to the variables in `ezbake.rb`

### Running

Running ezbake works much like any other Leiningen plugin or built-in task.
However, if you are working on a standalone project it will be necessary to use
a profile such as the `:ezbake` profile as shown in the configuration above.

#### `stage`

The stage action is useful for when you'd like to install an ezbake uberjar from
source or inspect its contents without waiting for the build step to complete.

```shell
lein with-profile ezbake ezbake stage
```

This will create an ephemeral git repository at `./target/staging` with staged
templates ready for consumption by the build step. If the project being staged
has a SNAPSHOT version, then a snapshot build will be deployed to the project's
configured snapshots repository (typically our internal nexus repository
server at `nexus.delivery.puppetlabs.net`), in order to ensure our builds are
reproducible. This can be avoided by setting the `EZBAKE_NODEPLOY` environment
variable to any value. (If you set this environment variable in a build
pipeline, you will be severely punished by the Angel of Build Reproducibility.)

If the project being staged has a SNAPSHOT version, then a snapshot build will
be deployed to the project's configured snapshots repository (typically our
internal nexus repository server at `nexus.delivery.puppetlabs.net`), in order
to ensure our builds are reproducible. This can be avoided by setting the
`EZBAKE_NODEPLOY` environment variable to any value.

If the project or any of its dependencies have a SNAPSHOT version, but
a deployed snapshot artifact matching that version cannot be found in the
configured repositories (for example, because they have only been installed
locally), then EZBake will throw an error to prevent an unreproducible build. If
you're testing local changes, you can avoid this check by setting the
`EZBAKE_ALLOW_UNREPRODUCIBLE_BUILDS` environment variable to any value. (If you
set this in a build pipeline, bad things will happen, possible to a future
version of yourself.)

#### `build`

```shell
lein with-profile ezbake ezbake build
```

This will do everything the `stage` action does and then call the external
builder defined for this project. Currently, the only builder supported is
[Puppetlabs' Packaging tool](https://github.com/puppetlabs/packaging).

#### `build` for PE

For PE builds, you need to set an environment variable PE_VER to reflect the
version of PE you're building a package for.  e.g.:

```shell
PE_VER=2016.3 lein with-profile ezbake ezbake build
```

#### `local-build`

```shell
lein with-profile ezbake ezbake local-build
```

This will do everything the `stage` action does and then call the local builder
defined for this project. This will build .deb and .rpm packages for the project
on your local machine using [FPM](https://github.com/jordansissel/fpm). To
build successfully you'll need the FPM gem installed. You'll also need java
and leiningen. To build RPMs you'll want to be on some sort of RPM-based system
as you need RPM build tools. To build RPMs for SLES you'll need to have 
the systemd-rpm-macros rpm installed. Building .debs doesn't require anything
special.

Packages will end up in the output directory. RPM targets can be overwritten
by setting the `MOCK` environment variable and deb targets can be overwritten
by setting the `COW` environment variable.

#### `build` with a different profile

```shell
lein with-profile ezbake,pe ezbake build
```

This is an example of how a project might differentiate between "foss" and "pe"
packages. The `:pe` profile may define different values for `:lein-ezbake` or for
anything that might be found in the `:ezbake` profile. This is primarily useful
for projects that need to build their PE and FOSS packages from the same
repository.

#### `manifest`

```shell
lein with-profile ezbake ezbake manifest
```

The manifest action is useful when comparing multiple ezbake builds without
actually building an artifact.

This generates a json file at `ext/build_metadata.json` which contains
information about the project. This information has project dependencies and
their versions, any dependencies of those dependencies, ezbake version, and the
git sha of the project being built. Downstream packaging tools may decide to
make use of this file to track what is in a build in a more programmatic manner
with this artifact.

Note: This step is automatically ran as part of the stage action and as a
result also ran in the build action.

### Packaging Configuration Files

By default, in the final packages produced by an ezbake build, there will be a "config" directory
(usually `/etc/puppetlabs/<ezbake-project-name>`) that contains all of the final
configuration files for the ezbake application.

#### bootstrap-source
The `bootstrap-source` setting can be one of:
* (Default) `:bootstrap-cfg`
* `:services-d`

If the `:bootstrap-source` setting is set to `:services-d`, there will also be an additional
configuration directory under the installation directory:
`/opt/puppetlabs/server/apps/{project-name}/config/`
This directory will then be used to hold a `services.d/` directory that contains bootstrap files.
Usually this will be for bootstrap entries the application could not run without, and which should
not be modified by the user. See [Bootstrap Files](#bootstrap_files)

EZBake will assemble the contents of this directory from two sources:

1. Config files embedded in the jars of any of the dependencies
2. Config files local to the ezbake project

#### Config files embedded in jars of dependencies

If the jar produced by any of the dependencies of the ezbake project contains a
directory called `ext/config/`, then any files therein will be included in the final
`conf.d` directory of the ezbake package.

This is useful if your project uses a "composite" ezbake build.  For example, we build
`pe-puppetserver` packages from a composite project called `pe-puppetserver`, which does
not contain any code; it's a project that just exists for use with ezbake, to compose
other things together.  One of the dependencies that it brings in is `puppet-server`,
which is the main codebase where all of the OSS Puppet Server code lives.  The puppet-server
jar includes some config files, such as `puppetserver.conf`, which are specific to Puppet
Server.  These config files won't change based on packaging, and it is useful/important
to keep them in sync with the related Puppet Server source code, so they live in the
upstream repos and ezbake retrieves them from the jar files.

#### Config files local to the ezbake project

EZBake supports a setting in the `:lein-ezbake` portion of your `project.clj` called `:config-dir`.
The default value if you do not provide this setting is `config`.  The value of this setting
tells ezbake where to look for local config files that may vary based on the packaging task
at hand; so, for example, files like `webserver.conf` and `web-routes.conf` may vary depending
on what services you are composing together at build time, so they are not guaranteed to be static
based on the upstream code.

To build on our `pe-puppetserver` example above; this composite ezbake project brings in services
like the file sync service and code manager, and we need to build out the `web-routes.conf` file
based on the list of all of the services that we are composing together.  Thus, this type of
config file needs to live in the repo of the ezbake build project itself, and wouldn't make sense
to try to include inside of the 'puppet-server' jar, since we could be building many permutations
of packages that contain Puppet Server and can't know ahead of time what a valid `web-routes.conf`
will look like.

#### tl;dr on where to put config files

The rule of thumb is:

* For config files that are specific to an individual project, such as the OSS Puppet Server project,
  the files should be embedded in the jar under `ext/config`.  e.g.: `ext/config/conf.d/puppetserver.conf`.
* For config files that are specific to the *packaging* task (usually this implies a composite ezbake build),
  the files should live in the repo of the ezbake packaging project; e.g. in the `pe-puppetserver` composite
  project, there is a directory at the root called `config`, and this will contain files like
  `config/conf.d/web-routes.conf`.

### Bootstrap Files
There are two ways of specifying a set of services to bootstrap your TK application with:

1. A single `bootstrap.cfg` file in your project's `:config-dir` directory
2. Two `services.d` directories which can contain any number of `*.cfg` files that will be
   merged together

This choice is controlled by the `:bootstrap-source` setting. The default value
of `:bootstrap-cfg` makes ezbake look for a single file called `bootstrap.cfg`
directly under your `:config-dir`. Setting `:booststrap-source` to `:services-d`
will cause ezbake to construct service init scripts that pass TK two directories,
usually `/etc/puppetlabs/{project-name}/services.d/`, and
`/opt/puppetlabs/server/apps/{project-name}/config/services.d`.

This means that in your TK project, you'll need to at least put a `service.d/` directory
in your `:config-dir`, and optionally another in your `:system-config-dir`. You might want to use
the `system-config-dir` if you are concerned about users editing files under `/etc`.

For example, with the default lein-ezbake settings, your directories might look like this:
```
.
├── config
│   └── services.d
│       ├── more_services.cfg
│       ├── other_services.cfg
│       └── some_services.cfg
└── system-config
    └── services.d
        └── really_important_services.cfg
```

### Subcommands

EZBake packages can install "subcommands" which can be run by the user via a
command-line interface (CLI).  The
![cli-app.erb](./resources/puppetlabs/lein-ezbake/staging-templates/cli-app.erb)
template defines the wrapper CLI "application" through which the subcommands are
run.  The `cli-app.erb` template is converted into a shell script which resides
at `/opt/puppetlabs/server/bin/<package-name>` after package install.
Subcommands are also implemented as erb templates, residing in separate files
under the
![cli resources](./resources/puppetlabs/lein-ezbake/template/global/ext/cli)
namespace.  The subcommand erb templates are converted into shell scripts which
reside at `/opt/puppetlabs/server/apps/<package-name>/cli/apps` after package
install.

A user can invoke a CLI subcommand by passing the name of the subcommand as the
first argument to the wrapper CLI "application".  For example, a user could
type the following to execute the `foreground` subcommand:

```shell
/opt/puppetlabs/server/bin/<package-name> foreground
```

A Clojure project which uses `lein-ezbake` as a plugin can provide its own
custom subcommands as erb templates under the project's `./resources/ext/cli`
directory.  The erb templates are converted into shell scripts at package
build time.

EZBake includes the following subcommands in every package which is built:

#### Foreground

This starts up the Trapperkeeper application under Java - similar to what the
service framework would do when `service <app> start` is run, only with the
application being run in the shell foreground rather than as a daemon.  stdout
and stderr output from the application will appear in the shell foreground,
as opposed to the daemon log or journal (which would happen when the application
is run as a daemon instead).

Any additional arguments given to the subcommand are passed along as
command-line options to the Java command line for the Trapperkeeper application.
For example, the following command line would run the application in
foreground in "debug" mode, using Trapperkeeper's
['--debug'](https://github.com/puppetlabs/trapperkeeper/blob/master/documentation/Command-Line-Arguments.md#command-line-arguments)
CLI argument:

```shell
/opt/puppetlabs/server/bin/<package-name> foreground --debug
```

When the `--debug` flag is given to the `foreground` subcommand, all
application log output is printed both to the standard log locations configured
in the application's global log configuration and to stdout for the shell
in which foreground is being run.

The Java application is run under the same user as the service framework would
use, set via the `user` setting in the project's EZBake configuration.

#### Start

This starts up the Trapperkeeper application as a background process.  This is
the same script which is invoked when the service is started via the service
framework.  Note that when the service is started is run via sourcing the 'start'
subcommand directly, though, that it will not be fully daemonized and runs as
whatever user the CLI subcommand is started with - not necessarily the same as
what is set for the `user` setting in the project's EZBake configuration.

Example:

```shell
/opt/puppetlabs/server/bin/<package-name> start
```

The script returns an exit code of 0 if the service is successfully started or
if the service had already been started before the script was run.  Note that
the 'start' subcommand does not start a second instance of the application if
one is already running.

The script returns an exit code of 1 if the service fails to start.  The script
waits for the server to start for up to the number of seconds specified by the
START_TIMEOUT environment variable, if specified, or the value of the
`start-timeout` setting in the project's EZBake configuration.  If this time
limit is exceeded, the script attempts to kill the service before exiting.

#### Stop

This stops any currently running instance of the Trapperkeeper application.
This is the same script which is invoked when the service is stopped via the
service framework.

Example:

```shell
/opt/puppetlabs/server/bin/<package-name> stop
```

The script returns an exit code of 0 if the service is successfully stopped or
if the service was already not running at the time the script is run.

The script returns an exit code of 1 if the service fails to stop.  The script
waits for the server to stop for up to the number of seconds specified by the
SERVICE_STOP_RETRIES environment variable, if specified, or the value of the
`stop-timeout` setting in the project's EZBake configuration.

#### Reload

This reloads any currently running instance of the Trapperkeeper application.
This is the same script which is invoked when the service is reloaded via the
service framework.

The reload is triggered by sending a SIGHUP signal to the Java process running
the Trapperkeeper service.  Trapperkeeper handles the SIGHUP by calling `stop`
on the application, followed by calling `start` on the application.  When the
`start` call has finished, Trapperkeeper increments a start counter in a
`restart-file` on disk.  The reload script polls in a loop for the contents of
the `restart-file`.  When the contents have changed, the reload script
determines that the reload is successful and the script returns.  For more
information on the `restart-file` feature in Trapperkeeper, see
![this page](https://github.com/puppetlabs/trapperkeeper/blob/1.5.1/documentation/Restart-File.md)
in the Trapperkeeper documentation.

Example:

```shell
/opt/puppetlabs/server/bin/<package-name> reload
```

The script returns an exit code of 0 if the service is successfully reloaded.

The script returns an exit code of 1 if the service fails to reload.  If the
service is stopped at the time the script is run, the script will return an
exit code of 1 without attempting to start the service.  If the service is
running and the SIGHUP signal can successfully be sent, the script waits for the
server to reload for up to the number of seconds specified by the RELOAD_TIMEOUT
environment variable, if specified, or the value of the `reload-timeout` setting
in the project's EZBake configuration.  The script returns an exit code of 1 if
the timeout is reached or if the process dies during the reload attempt.

### Testing

After building packages it is often necessary to install those packages in live
environments on the OSes supported by the ezbake templates. For this purpose
[Puppetlabs' Beaker](https://github.com/puppetlabs/beaker) is the, uh, choice
tool of discerning developers.

## Maintainers

See MAINTAINERS file
