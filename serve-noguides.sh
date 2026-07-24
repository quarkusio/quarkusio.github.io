#!/bin/sh
echo "WARNING: Serving site with no guides. It is fast though!"
QUARKUS_PROFILE=noguides mvn quarkus:dev "$@"
