#!/bin/bash
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
pushd "${SCRIPTDIR}"
echo "WARNING: Serving site with only the latest guides (latest and main)"
QUARKUS_PROFILE=only-latest-guides mvn quarkus:dev "$@"
popd
