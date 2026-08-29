#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1
echo "WARNING: Serving site with no guides. It is fast though!"
QUARKUS_PROFILE=noguides "${SCRIPTDIR}/mvnw" quarkus:dev "$@"
