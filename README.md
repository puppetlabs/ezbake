lein-ezbake
======

EZBake is a leiningen plugin that integrates multiple Trapperkeeper services
and their config files into a single uberjar and stages them in preparation for
packaging.

## Using

To get started using EZBake, please add it to the `:plugins` key in your
`project.clj`:

```clojure
{:plugins [[puppetlabs/lein-ezbake "0.1.0"]]}
```

Before you can get started using it, however, there is some additional
configuration necessary.

### Configuring

```clojure
{:lein-ezbake {
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
and [Composite Projects](#composite-projects); what follows here are brief
explanations of how to configure each type of project.

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

Since a standalone ezbake project (defines its own trapperkeeper service which
is included in the ezbake package) is often defined in the same repository as
one of the TK services it is intended to package and therefore shares a
`project.clj`, it may be necessary to overwrite some values used by ezbake when
creating the staging directory.

Specifically, the `:name` and `:dependencies` keys may need to be replaced as in
the [Puppet Server](https://github.com/puppetlabs/puppet-server) example below.
The `:name` replacement is necessary to avoid name collision when an ezbake
project also consists of clojure code that defines its own TK services and the
resulting package name is does not match the Maven artifact name.

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

Also note that the symbol `:ezbake` is strictly necessary here because
the rake taskes provided by `lein-ezbake` for [Puppetlabs' Packaging
tool](https://github.com/puppetlabs/packaging) use `with-profile ezbake` when
building an uberjar.

#### Composite

Composite ezbake projects (multiple maven artifacts/dependencies, no
trapperkeeper services defined) usually do not define their own services
but rather provide a list of dependencies which themselves define TK services.
Because of this it is
not strictly necessary to define an `:ezbake` profile on composite projects;
although it is conceivable that such a composite project may define its own
services, it is unlikely and ill-advised because no one likes blurred lines in
architectural diagrams. Just look at the Leaning Tower of Piza.

### Running

Running ezbake works much like any other Leiningen plugin or built-in task.
However, if you are working on a standalone project it will be necessary to use
the `ezbake` profile as shown in the configuration above.

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

### Testing

After building packages it is often necessary to install those packages in live
environments on the OSes supported by the ezbake templates. For this purpose
[Puppetlabs' Beaker](https://github.com/puppetlabs/beaker) is the, uh, choice
tool of discerning developers.

