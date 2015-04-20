## 0.2.8
 * Fix copy-paste error in EL init script template
 * Use templated :start_timeout value in Debian init scripts.

## 0.2.7
 * Add 'Should-Start' LSB headers to SUSE and EL init script templates
 * Add Fedora 21 build target
 * Remove Fedora 19 build target

## 0.2.6
 * Feature: Add a full lein dependency tree to the ezbake.manifest file that is
   included in packages. 

## 0.2.5
 * Bugfix: Foreground script now attempts to use the runuser command to switch
   to the service user if it's available. Otherwise su or sudo are used.

## 0.2.4
 * Feature: Ability to specify actions run as root before
   starting up a service.

## 0.2.3
 * Bugfix: Plumb $rubylibdir into redhat install step.
   * Standardize on "$rubylibdir" variable name in spec files.
 * Bugfix: Use project :real_name in FOSS postinstall.

## 0.2.2
 * Bugfix: Update path to install.sh for debian postins.
 * Bugfix: Fix bug where RPM specs were putting config files in the wrong place.
 * Feature: Add create-dirs key to [:lein_ezbake :vars] to enable creation of
   arbitrary directories with 0700 permissions.
 * Bugfix: Typo in install.sh.erb.
 * Bugfix: Add net-tools dependency for service unit files.

## 0.2.1
 * Bugfix: Pass through local-repo value when making aether requests.

## 0.2.0
 * Rewrite templates to share installation code between install-from-source and
   install-from-package.

## 0.1.0
 * Rewrite ezbake to follow leiningen plugin application model.
