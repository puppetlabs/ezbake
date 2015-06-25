## 0.3.11 - 2015-06-24

 * Fixes for service account handling on package
   and source based installations
 
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
