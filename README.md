lein-ezbake
======

EZBake is a leiningen plugin that integrates multiple Trapperkeeper services
and their config files into a single uberjar and stages them in preparation for
packaging.

## Using

To get started using EZBake, please add it to the `:plugins` key in your
`project.clj`:

```clojure
{:plugins [[puppetlabs/lein-ezbake "0.2.3"]]}
```

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
    :java-args "-Xms2g -Xmx2g -XX:MaxPermSize=256m"}}}

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

Composite ezbake projects usually do not define their own services but rather
provide a list of dependencies which themselves define TK services.  Because of
this it is not strictly necessary to define a profile such as `:ezbake` shown
above; although it is conceivable that such a composite project may define its
own services, it is unlikely and ill-advised because no one likes blurred lines
in architectural diagrams. Just look at the Leaning Tower of Pisa.

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
templates ready for consumption by the build step.

#### `build`

```shell
lein with-profile ezbake ezbake build
```

This will do everything the `stage` action does and then call the external
builder defined for this project. Currently, the only builder supported is
[Puppetlabs' Packaging tool](https://github.com/puppetlabs/packaging).

#### `build` with a different profile

```shell
lein with-profile ezbake,pe ezbake build
```

This is an example of how a project might differentiate between "foss" and "pe"
packages. The `:pe` profile may define different values for `:lein-ezbake` or for
anything that might be found in the `:ezbake` profile. This is primarily useful
for projects that need to build their PE and FOSS packages from the same
repository.

### Packaging Configuration Files

By default, in the final packages produced by an ezbake build, there will be a "config" directory
(usually `/etc/puppetlabs/<ezbake-project-name>`) that contains all of the final
configuration files for the ezbake application.

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

The default is to use a single `bootstrap.cfg` directly under your `:config-dir`. Setting
`:bootstrap-source` to `:services-d` in your ezbake settings will cause ezbake to construct
service init scripts that pass TK two directories, usually
`/etc/puppetlabs/{project-name}/services.d/`, and
`/opt/puppetlabs/server/apps/{project-name}/config/services.d`.

This means that in your TK project, you'll need to at least put a `service.d/` directory
in your `:config-dir`, and optionally another in your `:system-config-dir`. You might want to use
the `system-config-dir` if you are concerned about users editing files under `/etc`

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

### Testing

After building packages it is often necessary to install those packages in live
environments on the OSes supported by the ezbake templates. For this purpose
[Puppetlabs' Beaker](https://github.com/puppetlabs/beaker) is the, uh, choice
tool of discerning developers.

## Maintainers

Release Engineering at Puppet Labs.

Maintainer: Michael Stahnke <stahnma@puppetlabs.com>

Tickets: https://tickets.puppetlabs.com/browse/EZ/
