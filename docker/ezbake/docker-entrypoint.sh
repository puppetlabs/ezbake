#!/bin/bash

set -e

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
    echo '$EZBAKE_VERSION is required when $UPDATE_EZBAKE_VERSION=true'
    exit 1
  fi

  echo "Building with ezbake version $EZBAKE_VERSION"
  sed -i "s|puppetlabs/lein-ezbake \".*\"|puppetlabs/lein-ezbake \"$EZBAKE_VERSION\"|" project.clj
fi

lein clean && lein install

lein with-profile $LEIN_PROFILES ezbake local-build
rsync -a output/ /output/
