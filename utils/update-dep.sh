#!/usr/bin/env bash

FILE="${1:?}"
NEW_VERSION="${2:?}"
PROJECT_NAME="${3:?}"
NAMESPACE="${4:-puppetlabs}"

SED_ADDRESS="\[${NAMESPACE}\/${PROJECT_NAME} "
SED_REGEX="\".*\""
SED_REPLACEMENT="\"${NEW_VERSION}\""
SED_COMMAND="s|${SED_REGEX}|${SED_REPLACEMENT}|"

set -e
set -x

sed -i -e "/${SED_ADDRESS}/ ${SED_COMMAND}" ${FILE}
