#!/bin/bash

set -e

source /.docker_build_args

echo "Ezbake docker image $version, build $build_date, ref $vcs_ref, source $source_url"

# setup maven repository cache
if [ -d /repo ] ; then
  cp -na /root/.m2/repository/* /repo/
  rm -rf /root/.m2/repository
  ln -s /repo /root/.m2/repository
  echo '{:user {:local-repo "/repo"}}' > /root/.lein/profiles.clj
fi

if [ -n "$EZBAKE_REPO" ]; then
  echo "cloning $EZBAKE_REPO"
  git clone $EZBAKE_REPO /ezbake
  cd /ezbake
  if [ -n "$EZBAKE_BRANCH" ]; then
    echo "checkout origin/$EZBAKE_BRANCH"
    git checkout origin/$EZBAKE_BRANCH
  elif [ -n "$EZBAKE_REF" ]; then
    echo "checkout $EZBAKE_REF"
    git checkout $EZBAKE_REF
  fi
  lein clean && lein install

  if [ -z "$EZBAKE_VERSION" ] ; then
    export EZBAKE_VERSION=$(sed -rn 's@.*defproject .* "([^"]+)".*@\1@p' project.clj)
  fi
fi

if [ -n "$PROJECT_REPO" ]; then
  echo "cloning $PROJECT_REPO"
  git clone $PROJECT_REPO /workspace
  cd /workspace
  if [ -n "$PROJECT_BRANCH" ]; then
    echo "checkout $PROJECT_BRANCH"
    git checkout origin/$PROJECT_BRANCH
  elif [ -n "$PROJECT_REF" ]; then
    echo "checkout $PROJECT_REF"
    git checkout $PROJECT_REF
  fi
fi

cd /workspace

if [ "$UPDATE_EZBAKE_VERSION" == 'true' ]; then
  if [ -z "$EZBAKE_VERSION" ]; then
    # default to ezbake version in image
    export EZBAKE_VERSION=$version
    echo '$UPDATE_EZBAKE_VERSION=true but $EZBAKE_VERSION not set! Using ezbake version included in container image.'
  fi

  echo "Building with ezbake version $EZBAKE_VERSION"
  sed -i "s|puppetlabs/lein-ezbake \".*\"|puppetlabs/lein-ezbake \"$EZBAKE_VERSION\"|" project.clj
fi

lein clean && lein install

lein with-profile $LEIN_PROFILES ezbake local-build
rsync -a output/ /output/
