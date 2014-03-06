prefix = /usr/local
datadir = $(prefix)/share
confdir = /etc
initdir = $(confdir)/init.d
rubylibdir = $(shell ruby -rrbconfig -e "puts RbConfig::CONFIG['sitelibdir']")
rundir = /var/run

ifeq ($(wildcard /etc/redhat-release),/etc/redhat-release)
	defaultsdir = $(confdir)/sysconfig
	initsrc = ext/redhat/init
	rundir = /var/run/classifier
else
	defaultsdir = $(confdir)/default
	initsrc = ext/debian/classifier.init
endif

classifier.jar:
	lein uberjar
	mv target/classifier.jar classifier.jar

install-classifier: classifier.jar
	install -d -m 0755 "$(DESTDIR)$(datadir)/classifier"
	install -m 0644 classifier.jar "$(DESTDIR)$(datadir)/classifier"
	install -d -m 0755 "$(DESTDIR)$(confdir)/classifier"
	install -m 0644 ext/classifier.conf "$(DESTDIR)$(confdir)/classifier"
	install -d -m 0755 "$(DESTDIR)$(defaultsdir)"
	install -m 0644 ext/default "$(DESTDIR)$(defaultsdir)/classifier"
	install -d -m 0755 "$(DESTDIR)$(initdir)"
	install -m 0755 $(initsrc) "$(DESTDIR)$(initdir)/classifier"
	install -d -m 0755 "$(DESTDIR)$(rundir)"

install-terminus:
	install -d -m 0755 "$(DESTDIR)$(rubylibdir)/puppet/indirector/node"
	install -m 0644 puppet/lib/puppet/indirector/node/classifier.rb "$(DESTDIR)$(rubylibdir)/puppet/indirector/node/classifier.rb"
