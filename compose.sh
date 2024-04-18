export _SELINUX_=$(if [[ "$(uname)" == "Linux" ]]; then echo ":Z"; else echo ""; fi)
echo '$_SELINUX_'
docker-compose up $*
