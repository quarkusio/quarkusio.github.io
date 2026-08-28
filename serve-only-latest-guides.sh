#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1
echo "WARNING: Serving site with only the latest guides (latest and main)"
QUARKUS_PROFILE=only-latest-guides "${SCRIPTDIR}/mvnw" quarkus:dev "$@"
