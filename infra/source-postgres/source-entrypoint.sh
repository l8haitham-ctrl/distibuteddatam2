#!/bin/sh
set -eu
certs=/var/lib/postgresql/tls
mkdir -p "$certs"
if [ ! -s "$certs/server.key" ] || [ ! -s "$certs/server.crt" ]; then
  umask 077
  openssl req -new -x509 -nodes -newkey rsa:2048 -days 365 \
    -subj '/CN=postgres-source' -addext 'subjectAltName=DNS:postgres-source,DNS:localhost,IP:127.0.0.1' \
    -keyout "$certs/server.key" -out "$certs/server.crt" >/dev/null 2>&1
fi
chown postgres:postgres "$certs" "$certs/server.key" "$certs/server.crt"
chmod 600 "$certs/server.key"
exec /usr/local/bin/docker-entrypoint.sh "$@"
