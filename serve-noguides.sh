#!/bin/sh
echo WARNING: Serving site with no guides. It is fast though!
bundle exec jekyll serve --config _config.yml,_config_dev.yml,_noguides_config.yml $*

