#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1
"${SCRIPTDIR}/mvnw" quarkus:dev "$@"
