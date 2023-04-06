# Change Log

This changelog adheres to [Keep a CHANGELOG](http://keepachangelog.com/).

## [Unreleased]

## [2.4.1] - 2023-04-06
Bugfix:
  * EL OSes depending on java 8 used the wrong package name

## [2.4.0] - 2023-04-06
Added:
  * Add `puppet-platform-version` parameter, defaulting to 7, to allow EZBake
    to pick newer java versions for newer platforms
  * On Platform 8, Redhat 8 and Debian platforms will allow Java 11 or 17

Removed:
  * (EZ-149) Removed debian 9 from build targets
  * (maint) Removed pl-el-6-i386 and base-xenial-i386.cow as build defaults for foss.

## [2.3.2] - 2022-02-15
Added:
  * (RE-14499) Add sles15 as a FOSS and PE server build target
  * (RE-14541) Add ubuntu 20.04 as a FOSS and PE server build target

## [2.3.1] - 2022-01-21
Added:
  * (RE-14406) Added redhatfips-8-x86_64 as a platform for pe

## [2.3.0] - 2022-01-06
Removed:
  * Removed debian 8 from build targets

Added:
  * Use sha256 digests for rpm packages.
  * Add debian 11 to build targets

Bugfix:
  * (EZ-137) Changed templates to use /run directory instead of /var/run directory for PIDFiles except for with el-6 platform.
  * (EZ-143) Changed templates to use /run directory instead of /var/run directory for tmpfile.

## [2.2.4] - 2021-08-03
Bugfix:
  * (EZ-146) Update Jenkins locations; tell curl to follow Jenkins trigger redirects.

## [2.2.3] - 2021-03-31
Bugfix:
  * (EZ-142) Fix quoting of `-XX:OnOutOfMemoryError` argument added in 2.2.1.
  * (REPLATS-150) Skip efforts to run as user when already service user.
    In this case, the foreground command is already being executed as the
    service user and attempts to become that user fail as it doesn't have
    privileges to redundantly become itself anyway.

## [2.2.2] - 2021-02-19
Added:
  * (CPR-753) Changed permissions for logdir from 700 to 750.

Maintenance:
  * (RE-13425) Update the GPG signing key in the pe and foss template build_defaults.yaml

## [2.2.1] - 2020-08-21
Bugfix:
  * (RE-13643) EZ-132 broke java out-of-memory handling for those platforms not yet on Java 11.
    This fix introduces a check for Java major version >= 11 before
    setting `-XX:+CrashOnOutOfMemoryError` over `-XX:OnOutOfMemoryError="kill -9 %p"`

## [2.2.0] - 2020-8-17
Feature:
  * (EZ-140) Add configuration setting to allow dependencies to be excluded
    from the search for resources.
  * (RE-13571) Add support for ubuntu 20.04 (focal).

## [2.1.8] - 2020-07-10
Bugfix:
  * (EZ-35) Make use of project build-scripts, in addition to dependency build-scripts.
  * Revert fix from 2.1.7 adding scripts to the packaging postinst. This is handled
    in the install.sh script.

## [2.1.7] - 2020-06-16
Bugfix:
  * (EZ-135) PE packages should have a dependency on pe-bouncy-castle-jars.
  * Fix adding postinstall steps. Currently `postinst` or `postinst-install` do
    not make it into packaging scripts for rpm or deb systems.
Maintenance:
  * (RE-13517) (RE-13502) Remove ubuntu 16.04 (xenial) and el 6 from PE `build_defaults`.
  * Update build.rake to match current jenkins security practices.

## [2.1.6] - 2020-02-11
This is a bugfix release.

Bugfix:
  * (EZ-132) Use `-XX:+CrashOnOutOfMemoryError` instead of
    `-XX:OnOutOfMemoryError="kill -9 %p"`. Additionally, set a new log file for
    the crash in `-XX:ErrorFile=/var/log/puppetlabs/puppetserver/puppetserver_err_pid%p.log`.
  * (RE-12523) Don't set `TasksMax` in the Debian 8 service file since that option
    is not supported by the included version of systemd.

## [2.1.5] - 2019-10-08
This is a maintenance release.

Maintenance:
  * Remove redhatfips from default platforms for FOSS projects.
  * Documentation improvements.

## [2.1.4] - 2019-09-24
This is a maintenance release.

Maintenance:
  * Use Java 11 for Debian 10 (Buster).

## [2.1.3] - 2019-09-06
This is a maintenance release.

Maintenance:
  * (RE-12748) Add support for Debian 10 (Buster).

Changed:
  * Set locale to utf8 in ezbake container

## [2.1.2] - 2019-08-26
This is a bugfix release.

Bugfix:
  * Extract name from `bin-files`, which are JarFileEntries, before converting to Ruby value.

## 2.1.1 - 2019-08-21
This is a maintenance release.

Maintenance:
  * (SERVER-2602) Do not hard-code egd in included subcommand files. Users that would like to manage egd source may do so via `JAVA_ARGS` or their `java.security` file.
  * Stop calling deprecated method `Pkg::Util::Git.fail_on_dirty_source`.

## 2.1.0 - 2019-08-13
This is a bugfix and feature release

Feature:
  * (RE-12609) Add support for `start-before` dependencies for sysv and systemd.
  * (PDB-4373) Add `as-ruby-literal` function to generate properly escaped ruby to
    the ezbake.rb mustache template. All formatting that was happening in that template
    has been moved to `as-ruby-literal`.
  * Add artifacts for building ezbake docker image. Image is published on commit to puppet/ezbake
    on dockerhub.

Bugfix:
  * Update package descriptions to use newlines.
  * Fix missing `start-after` support for deb-based systemd systems.
  * Add 'Wants' to systemd 'After' units so we don't fail on split installs.

## 2.0.4 - 2019-07-02
This is a bugfix release.

Bugfix:
  * Fix adding additional preinstall tasks to rpm builds. Previously, any
    specified preinstall tasks were ignored for rpms.
  * Set `file` protocol when using the `-Djava.security.egd` flag.

## 2.0.3 - 2019-05-29
This is a maintenance release.

Maintenance:
  * (RE-12428) Add support for redhatfips-7-x86_64.

## 2.0.1 - 2019-04-23
This is a maintenance release.

Maintenance:
  * Remove ubuntu 14.04 (trusty) and sles 11 from PE build_defaults.

## 2.0.0 - 2019-03-12
This is a backwards-incompatible release.
  * (RE-12093) Remove old rpm and deb artifacts previously used to build packages.
  * Remove legacy-build action that used the removed artifacts. Packages can only
  be built using FPM.

## 1.9.7 - 2019-03-04
This is a bugfix release.

Bugfix:
  * (RE-12082) Return to using `adduser` commands instead of `libuser` commands.
    This reverts the work done for PE-24606 in the 1.9.5 release.

## 1.9.6 - 2019-02-25
This is a maintenance release.

Maintenance:
  * (RE-12081) Add support for el-8-x86_64.

## 1.9.5 - 2019-02-12
This is a maintenance release.

Maintenance:
  * Update comments to reflect that we only support JRE 8 in some of our products.
  * (PE-24606) Use `luseradd` instead of `useradd` to fix systemd-tmpfiles-setup failure during reboot due to ldap dependencies of the puppet userid/groups.


## 1.9.4 - 2019-01-11
This is a bugfix release.

Bugfix:
  * Don't override `JAVA_ARGS_CLI` command line settings when loading defaults
    for PE projects.

## 1.9.3 - 2019-01-09
This is a bugfix release.

Bugfix:
  * Don't override `JAVA_ARGS_CLI` command line settings when loading defaults.
    Edit: This was only added for FOSS projects.

## 1.9.2 - 2019-01-08
This is a maintenance release.

Maintenance:
  * (EZ-112) Use `nss-lookup` rather than `network-online` to ensure networking
    is online before starting a service.
  * (SERVER-2399) Add `java-args-cli` variable to allow for configuring
    non-service-related commands.

## 1.9.1 - 2018-12-10
This is a maintenance release.

Maintenance:
  * (EZ-128) Add `numeric-uid-gid` option to allow for specifying a desired
    uid/gid. We have a reserved uid/gid for the 'puppet' user, so this option
    allows us to explicitly set that. Note that this only affects rpm packaging.
  * (EZ-129) Set `TasksMax` to 4915, since some versions of systemd set
    `TasksMax` to 512, which is not enough for certain applications.

## 1.9.0 - 2018-11-28
This is a feature release.

Feature:
  * (EZ-127) Add docker-build to build an image from a clojure repo

## 1.8.10 - 2018-11-06
This release includes no changes from 1.8.9.
The ezbake release pipeline was accidentally kicked off, causing an extra tag.

## 1.8.9 - 2018-11-02
This is a maintenance release.

Maintenance:
  * Add fpm to Gemfile that gets included in package builds.
  * Hard-code `rubylibdir` variable in install script to avoid calling puppet's
    ruby, which doesn't include bundled dependencies.

## 1.8.8 - 2018-09-11
This is a bugfix and maintenance release.

 Bugfix:
  * Only produce authentication error message if `JENKINS_USER_AUTH` is unset.

 Maintenance:
  * (RE-109140) Detect if job creation fails and print helpful message.
  * Move authentication message to `get_auth_info` method.

## 1.8.7 - 2018-08-22
This is a bugfix release.

 Bugfix:
  * (EZ-125) bundle install prior to calling packaging rake tasks.
  * Include Gemfile in resulting package builds.

## 1.8.6 - 2018-08-14
This is a maintenance release.

Maintenance:
  * Add openjdk-10+ support.
  * (RE-10177) Use packaging as a gem.

## 1.8.5 - 2018-07-11
This is a maintenance release.

Maintenance:
  * Add support for Ubuntu 18.04 (Bionic).

## 1.8.4 - 2018-06-11
This is a bugfix release.

Bugfix:
  * (EZ-124) Use consistent timestamps throughout the `stage` process.

## 1.8.3 - 2018-04-17
This is a feature release.

Feature:
 * (EZ-20) Added support for TrapperKeeper arguments that can be used to adjust
 the flags passed to daemons and command line tools by setting `TK_ARGS`.

## 1.8.1 - 2018-03-07
This is a bugfix release.

Bugfix:
 * (EZ-112) Don't start puppetserver until networking comes online.
 * The ca-certificates package should be a build-time dependency not a runtime
   dependency.
 * The rpm parameter for rundir was getting set to the logdir instead of the
   rundir.
 * File mode for the Application Data Dir was not getting set correctly.
 * Missing %?{systemd_requires} for SLES packages.
 * Missing dependency on adduser for deb packages.
 * Debian triggers were only being added to RPMs.
 * Descriptions were not getting added to packages.

## 1.8.0 - 2018-02-14
This is a feature and bugfix release.

Feature:
  * (CPR-515) Add support for setting postinstall actions that should happen on
    initial install only.

Bugfix:
  * Reverts commit setting `build_tar: FALSE` in foss build defaults. This fix
    was masking a bug in github.com/puppetlabs/packaging which has since been
    fixed.

## 1.7.5 - 2018-01-09
This is a bugfix release.

Bugfix:
  * (SERVER-2068) Improve logic for determining the process ID for stop/start/
    reload commands to make it less likely to pick up an incorrect process.

## 1.7.4 - 2017-12-18
This is a maintenance release.

Maintenance:
  * Add "repo_name" and "nonfinal_repo_name" as targets for Packaging 1.0.x

## 1.7.3 - 2017-12-15
This is a maintenance release.

Maintenance:
  * Set `build_tar: FALSE` in foss build defaults in order to not attempt tar
    signing when undesired.

## 1.7.2 - 2017-12-12
This is a maintenance release.

Maintenance:
  * Use internal artifactory server instead of internal nexus server.
  * Update to [packaging](https://github.com/puppetlabs/packaging) 1.0.x to pick up
    changes to make it more straightforward to ship to the platform 5 repos,
    and to pick up the branch that's most actively maintained.

## 1.7.1 - 2017-11-29
This is a bugfix release.

Bugfix:
  * Generate valid replaces/conflicts in RPM and deb packaging
  * Pass MOCK and COW environment variables through to the jenkins job during
    a `lein ezbake build`.
  * If `repo-target` is unspecified debian artifacts should end up under
    `deb/<platform>` instead of `deb/<platform>/main`.


## 1.7.0 - 2017-11-14
This is a feature release.

Feature:
  * (EZ-111) Add support for RPM triggers. You can now add postinstall triggers
    that run on either installs or upgrades via the
    `redhat-postinst-install-triggers` or `redhat-postinst-upgrade-triggers`
    variables under `:lein-ezbake` in your project.clj. These variables are
    arrays of hashes in the format [ { :package "package", :scripts ["script 1", "script2"] } ]
  * (EZ-113) Add support for Debian triggers. This adds support for activate
    triggers via the `debian-activated-triggers` variable. This variable takes
    an array of trigger names. This also adds support for interest triggers for
    either install or upgrade via the `debian-interested-install-triggers` and
    `debian-interested-upgrade-triggers` variables. These variables are arrays
    of hashes in the format [ { :interest-name "trigger", :scripts ["script1", "script2"] } ]
    All of these variables are set under `:lein-ezbake` in your project.clj.

## 1.6.7 - 2017-11-29
This is a bugfix release.

Bugfix:
  * If `repo-target` is unspecified debian artifacts should end up under
    `deb/<platform>` instead of `deb/<platform>/main`.

## 1.6.6 - 2017-11-20
This is a bugfix release.

Bugfix:
  * Generate valid replaces/conflicts in RPM and deb packaging
  * Pass MOCK and COW environment variables through to the jenkins job during
    a `lein ezbake build`.

## 1.6.5 - 2017-11-14
This is a bugfix release.

Bugfix:
  * Pass `replaces-pkgs` from ezbake config to FPM packaging
  * Pass `create-dirs` from ezbake config to FPM packaging

## 1.6.4 - 2017-11-10
This release was incorrectly published as 1.6.4. Do not use this release.

## 1.6.3 - 2017-10-02
This is a bugfix release.

Bugfix:
  * We were incorrectly setting the package release to the package version if
    it was not a SNAPSHOT build. The release is now set to 1 unless we have a
    SNAPSHOT build. Behavior for SNAPSHOT builds is unchanged.

## 1.6.2 - 2017-09-25
This is a bugfix release.

Bugfix:
  * We were generating invalid bash (empty if block) if there were no additional
    dependencies specified.
  * We weren't keeping the output tarball with the packaging artifacts.

## 1.6.1 - 2017-09-15
This is a bugfix and maintenance release.

Bugfix:
  * There was a bad version check that caused sles 12 packages to not get init
    scripts. This has been fixed.
  * We were attempting to reuse packaging artifacts but were deleting them after
    each platform was packaged. Moved the delete to after all platforms ran.

Maintenance:
  * Clean up debug output to accurately represent the options hash.
  * Update `help` output to print correct platforms
  * Print the URL for where the packages are going to be staged at the end of
    the `build` step.

## 1.6.0 - 2017-09-13
This is a feature and maintenance release.

Feature:
  * Added `local-build` task to allow building ezbake projects on infrastructure
    outside of Puppet. There is more information available in the README but
    this addition enables package building on any machine/VM/container/etc with
    the necessary dependencies installed.
  * Changes to `build` task to let it use new infrastructure and CI systems.
    These changes should be transparent with the exception of needing to pass
    jenkins authorization at runtime. This should be passed in the
    JENKINS_USER_AUTH environment variable as either '<job token>' or
    '<ldap username>:<personal auth token>'.
  * Added `legacy-build` task to preserve the `build` task from previous versions
    of ezbake.

Maintenance:
  * Document installation has been moved into the install.sh script. We were
    previously installing the docs with `%doc` entries and the doc control file.
    The `install.sh` script now installs the docs in the os-specific location to
    enable us to more easily change how we are packaging.

## 1.5.2 - 2017-08-29
Maintenance:
  * Removes Ubuntu Yakkety and Ubuntu Precise from the default COW list as those
    platforms are EOL.

## 1.5.1 - 2017-7-13
Bugs:
  * Get dependencies from requirements which do not have version numbers
    associated with it, as well parent dependencies.

## 1.5.0 - 2017-6-30
This is a feature release.

Feature:
  * (RE-8861) Generate metadata files when building projects.
    Adds a new `manifest` command that creates an 'ext/build_metadata.json' file
    containing dependencies and related metadata to aid in tracking down
    differences between packages built with ezbake.
    This file is also created during the `stage` and `build` actions.

## 1.4.0 - 2017-5-18
This is a feature, maintenance, and bug fix release.

Feature:
  * (RE-8726) Add support for specifying nonfinal vs. final repo targets and
    names.

Maintenance:
  * (RE-4844) Merge together content from the FOSS and PE ezbake templates.

Bugs:
  * (EZ-109) Eliminate some unnecessarily duplicated strings from the
    ezbake.manifest file.
  * (EZ-110) Retain any qualifying attributes (e.g., exclusions) when expanding
    a SNAPSHOT version for a coordinate, allowing exclusions to be reflected
    properly in the resulting ezbake.manifest file.
  * (EZ-110) For an `additional-uberjar`, build the immediate jar into the final
    uberjar even when the project has defined its own `uberjar` profile.

## 1.3.0 - 2017-5-3
This is a feature release
  * (SERVER-1772) Add support for building and installing additional uberjars.
    An `:additional-uberjars` EZBake setting has been added which allows projects
    to specify a list of versioned dependencies that will be built and installed
    next to the projects own uberjar.
  * (SERVER-1772) Support installing a `cli-defaults.sh` file which can be
    configured by EZBake projects and supply defaults for bash variables in a
    project's cli scripts
  * (EZ-108) Add support for `EZBAKE_ALLOW_UNREPRODUCIBLE_BUILDS` environment
    variable that when defined, will allow staging with undeployed SNAPSHOT
    versions

## 1.2.1 - 2017-4-20
  * (SERVER-1782) Fix for the openjdk8 package name in SLES-12

## 1.2.0 - 2017-4-12
This release removes Java7 support and changes the way that snapshots are deployed

Feature:
  * Remove Java 7 compatibility. Builds now assume Java (openjdk) 8
  * Change EZBake's stage command to:
      * Deploy an artifact to the configured snapshots repository when staging a project with
        a snapshot version.
      * List the deployed snapshot artifact's version as the project's version number in the
        ezbake.manifest & project_data.yaml files.
      * Resolve all dependencies with 'SNAPSHOT' versions to get a deployed snapshot artifact
        from the repository and list that artifact's version number in the ezbake.manifest &
        project_data.yaml. If no deployed snapshot artifacts can be found for the listed
        snapshot version, then an error is thrown and staging is aborted, to prevent
        unreproducible builds.
      * List each dependency's group as well as its name in the ezbake.manifest &
        project_data.yaml

## 1.1.8 - 2017-3-22
  * (SERVER-1763) Adds ca-certificates as a build dependency

## 1.1.7 - 2017-3-13
  * (SERVER-1472) The init scripts should now handle stop and restart operations
     better when the timeout value is exceeded.

## 1.1.6 - 2017-01-12
  * (CPR-400) Require `which` for RPM-based systems.

## 1.1.5 - 2016-11-29
  * Bugfix: (SERVER-1670) Stop autogenerating package dependencies in RPM
     packages - sets "AutoReq: 0"

## 1.1.4 - 2016-11-11
  * (EZ-102) Install SysV files in addition to SystemD files only on
    debian-ish platforms

## 1.1.3 - 2016-10-19
  * (EZ-104) Re-add the XXOutOfMemoryError java arg

## 1.1.2 - 2016-10-13
  * (EZ-103) Create rundir with correct permissions

## 1.1.0 - 2016-09-29

This is a minor feature release, with some maintenance work included.

Feature:

  * (SERVER-1412) Add support for `build-scripts` directory in projects; contents
    will be included in the build tarball for use in packaging tasks, but will not
    be included in final package.

Maintenance:

  * Improvements to validation of ezbake config during staging
  * Run `lein install` before stage, to ensure that we pick up local changes
  * Set umask to 027 in CLI scripts

## 1.0.0 - 2016-09-29
  * (EZ-56) Add service 'reload' to init scripts
  * (EZ-68) Add 'restart-file' ezbake option and reload app subcommand
  * (EZ-70) Modify service 'start' to use new 'start' app subcommand
  * (EZ-88) Support per-project additional build dependencies and adding
  the full contents of the project data directory into packaging
  * (EZ-90) Add 'start' app subcommand
  * (EZ-99) Add 'stop' app subcommand and modify service 'stop' command to use it
  * (EZ-100) Add timeout EzBake and defaults config settings for 'reload' and 'stop'

## 0.5.1 - 2016-09-13
* Update to use the new Puppet GPG key for package signing

## 0.5.0 - 2016-08-31
* Add `:logrotate-enabled` setting to allow disabling of logrotate.
  Enabled by default

## 0.4.4 - 2016-08-22
* Update FOSS build templates to work on SLES
* Update branching strategy to point to wiki
* Backport removal of heap dump to stable branch
* Modify project.clj for publishing to clojars

## 0.4.3 - 2016-07-28
 * Add configuration of open file limit to services
 * Set umask 027 on service startup
 * Fix issue where EL init script could create an empty pidfile

## 0.4.2 - 2016-05-20
  * Fix a bug in the new bootstrap `services.d` mode, for compatibility with systemd

## 0.4.1 - 2016-05-13
  * Update build-defaults to build for Ubuntu 16.04 (Xenial)
  * Fixed debian init script naming

## 0.4.0 - 2016-05-11
  * Add Ubuntu 15.10 (Wily) to default build targets
  * Add Ubuntu 16.04 (Xenial) to default build targets
  * Add Debian systemd script support
  * Add split bootstrap functionality to allow for user-configurable service
  entries alongside services that shouldn't be modified.
  * Changed folder structure for staging to add an ezbake/system-config dir

## 0.3.25 - 2016-04-26
  * Change Java dependency for deb platforms to non-virtual package

## 0.3.24 - 2016-04-20

  * Add updated maintainers information in README
  * Remove Fedora21 from default mocks
  * Add 5 second sleep to wait_for_app to help with race conditions on start

## 0.3.23 - 2016-02-18

  * Add restart on failure functionality to systemd services
  * Improved handling of mirrors and/or local repos for dependency resolution

## 0.3.22 - 2016-02-09

  * Update permissions for "projconfdir".

## 0.3.21 - 2015-11-10

  * Update to publish to clojars instead of internal repository.
  * Manage logfile ownership for SLES11 init scripts

## 0.3.20 - 2015-11-03

  * Increase default service startup timeout to 300 seconds
    (5 minutes) to avoid intermittent timeouts in testing.

## 0.3.19 - 2015-10-29

  * Updates to allow pulling in an ezbake.conf from
    the immediate project jar in addition to using the file
    from upstream dependencies.
  * Removes Ubuntu Utopic (14.10) and Fedora 20 build targets

## 0.3.18 - 2015-8-11

  * Fix unterminated 'if' in RPM spec template

## 0.3.17 - 2015-08-10

  * Update permissions on ezbake-functions.sh

## 0.3.16 - 2015-08-06

  * Fix typo (unclosed %if) in the PE rpm spec file

## 0.3.15 - 2015-08-06

  * RPM scriptlet fixes and cleanups
  * Fix RPM packaging for arbitrary dirs using create-dirs
  * Drop unused create-varlib function
  * Update permissions on ezbake-functions.sh
  * Stop hardcoding heap dumps in the init scripts

## 0.3.14 - 2015-07-10

 * Do not obsolete/provide with termini packaging for rpms

## 0.3.13 - 2015-07-06

 * Debian/Ubuntu should now correctly restart the process on upgrade
   if necessary.
 * Set the Vendor string in the RPM templates.

## 0.3.12 - 2015-07-01

 * Ubuntu PE init script template now creates
   PID directory correctly
 * More init script cleanups and synchronizing
   PE and FOSS templates

## 0.3.11 - 2015-06-26

 * Fixes for service account handling on package
   and source based installations
 * Update default startup timeout to 180 seconds
 * Remove EL-5 as a build target
 * Add support for systemd tmpfiles.d configs

## 0.3.10 - 2015-06-23

 * On package upgrade, update service account information (home dir,
   group membership, etc) if necessary

## 0.3.9 - 2015-06-18

 * Packaging: Fix varlibdir to use real_name for PE (so its
   app_data/lib/puppetdb, not app_data/lib/pe-puppetdb)
 * Packaging: In Debian, ignore service stops for services that are already
   stopped during upgrade.
 * Packaging: In Debian, add a prerm section to stop services gracefully on a
   failed upgrade.
 * Packaging: Fixed lots of inconsistencies between PE and FOSS, now we are
   closer then ever.
 * Packaging: Users were being created using the old FOSS based homedirs.
 * Packaging: Removal of log files for FOSS during package uninstall removed old
   non-AIO log file dirs.
 * Packaging: sharedstatedir and localstatedir were no longer used, app_data is
   preferred so these have been removed.
 * Packaging: call install.sh in PE using exec
 * Packaging: Debian with PE was not using the group to set permissions, default
   file and init set correctly.
 * Packaging: Tighten up permissions on application data directories, no longer
   world-readable.

## 0.3.8 - 2015-06-05

 * Packaging: Correct termini install.sh rubylibdir fallback detection for
   source based builds to work.
 * Packaging: Make rubylibdir setting in packaging consistent between PE/FOSS
   and Debian/Redhat.
 * Packaging: Fix: rundir should be created by the rpm package on install/upgrade
 * Packaging: Make install.sh use "localstatedir" variable instad of hardcoding '/var'

## 0.3.7 - 2015-05-29

 * Packaging: Add Ubuntu Precise and SLES for PE builds

## 0.3.6 - 2015-05-21
 * Bugfix: Set sudo HOME for foreground
 * Packaging: Do not build stable or testing for debian
 * Packaging: Set default cow to precise

## 0.3.5 - 2015-05-13
This release updates PE templates for AIO paths

 * Update PE packaging templates to use the AIO paths as specified in
   http://git.io/vUXTv
 * PE packaging templates now depend on 'puppet-agent' where appropriate

## 0.3.4 - 2015-04-24
 * Bugfix: Quiet install.sh debug output by default (can be overridden by
   setting EZ_VERBOSE). It is still enabled for package builds.
 * Update FOSS debian templates to allow the use java 8 if java 7 is not
   available
 * Add Ubuntu Utopic, Debian Jessie as build targets by default
 * Add 'Should-Start' LSB headers to SUSE and EL init script templates
 * Add Fedora 21 build target
 * Fix copy-paste error in EL init script template
 * Use templated :start_timeout value in Debian init scripts.

## 0.3.3 - 2015-04-15
 * Remove Fedora 19 build target

## 0.3.2 - 2015-03-30
 * (EZ-34) Allow ezbake to set apt/yum repo_name to ship to alternate repos

## 0.3.1 - 2015-03-25
 * Feature: Add a full lein dependency tree to the ezbake.manifest file that is
   included in packages.
 * Feature: Support for Fedora 21 as a build target.
 * Bugfix: Update to AIO layout for debian7, ubuntu1404, ubuntu1204 platforms.

## 0.3.0 - 2015-03-12
This release contains bug fixes and AIO path changes.

 * (SERVER-358) Update owner/group of the pid dir and add data dir (599d733)
 * (SERVER-344) Remove quotes around sudo command (070bac0)
 * (SERVER-344) Remove hardcoded puppet user (c13648a)
 * (SERVER-344) Choose best method to become puppet (1335712)
 * (PE-8274) Update oomkill parameter for systemd (f0bc4e9)
 * (SERVER-369) Update run directory for EL-7 (4625cd7)
 * (SERVER-369) Update ezbake to use new AIO directories (2eb3628)
 * (SERVER-387) Update to AIO server confdir layout (ee0a593)

## 0.2.14 - 2016-09-13
 * Packaging: Use new key for package signing

## 0.2.13 - 2016-08-31
 * Feature: Add `:logrotate-enabled` setting to allow disabling of logrotate.
   Enabled by default.

 Note: While technically a minor feature release, this is going to be a Z
 release to avoid branch renaming hassles.

## 0.2.12 - 2016-06-13
 * Backport changes to avoid setting -XX:+HeapDumpOnOutOfMemoryError by default

## 0.2.11 - 2016-03-18
 * Update to publish to clojars instead of internal repository
 * Packaging: Removes Fedora 20 build targets

## 0.2.10 - 2015-05-21
 * Bugfix: Set sudo HOME for foreground
 * Packaging: Do not build stable or testing for debian
 * Packaging: Set default cow to precise

## 0.2.9 - 2015-04-23
 * Bugfix: Quiet install.sh debug output by default (can be overridden by
   setting EZ_VERBOSE). It is still enabled for package builds.

## 0.2.8 - 2015-04-20
 * Fix copy-paste error in EL init script template
 * Use templated :start_timeout value in Debian init scripts.

## 0.2.7 - 2015-04-13
 * Add 'Should-Start' LSB headers to SUSE and EL init script templates
 * Add Fedora 21 build target
 * Remove Fedora 19 build target

## 0.2.6 - 2015-03-20
 * Feature: Add a full lein dependency tree to the ezbake.manifest file that is
   included in packages.

## 0.2.5 - 2015-03-12
 * Bugfix: Foreground script now attempts to use the runuser command to switch
   to the service user if it's available. Otherwise su or sudo are used.

## 0.2.4 - 2015-03-06
 * Feature: Ability to specify actions run as root before
   starting up a service.

## 0.2.3 - 2015-02-20
 * Bugfix: Plumb $rubylibdir into redhat install step.
   * Standardize on "$rubylibdir" variable name in spec files.
 * Bugfix: Use project :real_name in FOSS postinstall.

## 0.2.2 - 2015-02-17
 * Bugfix: Update path to install.sh for debian postins.
 * Bugfix: Fix bug where RPM specs were putting config files in the wrong place.
 * Feature: Add create-dirs key to [:lein_ezbake :vars] to enable creation of
   arbitrary directories with 0700 permissions.
 * Bugfix: Typo in install.sh.erb.
 * Bugfix: Add net-tools dependency for service unit files.

## 0.2.1 - 2015-02-05
 * Bugfix: Pass through local-repo value when making aether requests.

## 0.2.0 - 2015-02-03
 * Rewrite templates to share installation code between install-from-source and
   install-from-package.

## 0.1.0 - 2015-01-13
 * Rewrite ezbake to follow leiningen plugin application model.

[Unreleased]: https://github.com/puppetlabs/ezbake/compare/2.3.2...HEAD
[2.3.2]: https://github.com/puppetlabs/ezbake/compare/2.3.1...2.3.2
[2.3.1]: https://github.com/puppetlabs/ezbake/compare/2.3.0...2.3.1
[2.3.0]: https://github.com/puppetlabs/ezbake/compare/2.2.4...2.3.0
[2.2.4]: https://github.com/puppetlabs/ezbake/compare/2.2.3...2.2.4
[2.2.3]: https://github.com/puppetlabs/ezbake/compare/2.2.2...2.2.3
[2.2.2]: https://github.com/puppetlabs/ezbake/compare/2.2.1...2.2.2
[2.2.1]: https://github.com/puppetlabs/ezbake/compare/2.2.0...2.2.1
[2.2.0]: https://github.com/puppetlabs/ezbake/compare/2.1.8...2.2.0
[2.1.8]: https://github.com/puppetlabs/ezbake/compare/2.1.7...2.1.8
[2.1.7]: https://github.com/puppetlabs/ezbake/compare/2.1.6...2.1.7
[2.1.6]: https://github.com/puppetlabs/ezbake/compare/2.1.5...2.1.6
[2.1.5]: https://github.com/puppetlabs/ezbake/compare/2.1.4...2.1.5
[2.1.4]: https://github.com/puppetlabs/ezbake/compare/2.1.3...2.1.4
[2.1.3]: https://github.com/puppetlabs/ezbake/compare/2.1.2...2.1.3
[2.1.2]: https://github.com/puppetlabs/ezbake/compare/2.1.1...2.1.2
