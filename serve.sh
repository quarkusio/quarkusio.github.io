#!/bin/sh
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
${SCRIPTDIR}/mvnw quarkus:dev "$@"
