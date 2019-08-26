FROM centos:7 as build
ARG ruby_major_ver=2.6
ARG ruby_patch_ver=2
ENV ruby_ver="$ruby_major_ver.$ruby_patch_ver"

RUN yum -y groupinstall "Development Tools" && \
    yum -y install wget openssl-devel

RUN wget "http://cache.ruby-lang.org/pub/ruby/$ruby_major_ver/ruby-$ruby_ver.tar.gz" && \
    tar xf "ruby-$ruby_ver.tar.gz"

WORKDIR /ruby-$ruby_ver
RUN ./configure && \
    make && \
    make install

FROM centos:7

ARG sles_12_mirror=http://osmirror.delivery.puppetlabs.net/sles-12-sp2-x86_64/RPMS.os
ARG version="2.0.4"
ARG vcs_ref
ARG build_date

ENV LANG="en_US.utf8"
ENV EZBAKE_ALLOW_UNREPRODUCIBLE_BUILDS=true
ENV EZBAKE_NODEPLOY=true
ENV EZBAKE_VERSION="$version"
ENV GEM_SOURCE=https://rubygems.org
ENV LEIN_PROFILES=ezbake

LABEL org.label-schema.maintainer="Puppet Release Team <release@puppet.com>" \
      org.label-schema.vendor="Puppet" \
      org.label-schema.url="https://github.com/puppetlabs/ezbake" \
      org.label-schema.name="ezbake" \
      org.label-schema.license="Apache-2.0" \
      org.label-schema.version="$EZBAKE_VERSION" \
      org.label-schema.vcs-url="https://github.com/puppetlabs/ezbake" \
      org.label-schema.vcs-ref="$vcs_ref" \
      org.label-schema.build-date="$build_date" \
      org.label-schema.schema-version="1.0" \
      org.label-schema.dockerfile="/Dockerfile"

COPY --from=build /usr/local/bin /usr/local/bin
COPY --from=build /usr/local/include /usr/local/include
COPY --from=build /usr/local/lib /usr/local/lib
COPY --from=build /usr/local/share /usr/local/share

RUN yum clean all && \
    yum install --assumeyes rpm-build java-1.8.0-openjdk-devel git curl ruby ruby-devel gcc-c++ make zlib-devel && \
    yum localinstall --assumeyes $sles_12_mirror/systemd-rpm-macros-3-8.374.noarch.rpm && \
    git config --global user.name "Puppet Release Team" && \
    git config --global user.email "release@puppet.com"
RUN curl --output /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && \
    chmod 0755 /usr/local/bin/lein && \
    /usr/local/bin/lein
RUN gem install --no-doc bundler fpm && \
    mkdir /workspace && \
    mkdir /ezbake_src
COPY docker/ezbake/docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

# Have the latest ezbake installed 
COPY . /ezbake_src
WORKDIR /ezbake_src
RUN lein clean && lein install

WORKDIR /

ENTRYPOINT ["/docker-entrypoint.sh"] 

COPY docker/ezbake/Dockerfile /
