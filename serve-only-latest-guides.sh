#!/bin/bash

# Make sure our working directory is where this script lives
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
pushd "${SCRIPTDIR}"

echo "WARNING: Serving site with only the latest guides (latest and main)"
bundle exec jekyll serve --config _config.yml,_config_dev.yml,_only_latest_guides_config.yml $*

popd
