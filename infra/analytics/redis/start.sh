#!/bin/sh
set -eu
umask 077
test "${#REDIS_ADMIN_PASSWORD}" -ge 16
test "${#REDIS_READER_PASSWORD}" -ge 16
test "$REDIS_ADMIN_PASSWORD" != "$REDIS_READER_PASSWORD"
admin_hash=$(printf %s "$REDIS_ADMIN_PASSWORD" | sha256sum | cut -d ' ' -f 1)
reader_hash=$(printf %s "$REDIS_READER_PASSWORD" | sha256sum | cut -d ' ' -f 1)
{
  printf 'user default off\n'
  printf 'user atlas_admin on #%s ~* +@all\n' "$admin_hash"
  printf 'user atlas_reader on #%s ~analytics:customer_segments:* +@read +scan +ping +info +select +client|setname +client|setinfo\n' "$reader_hash"
} > /data/users.acl
chown redis:redis /data/users.acl
# Local disposable fixture: no host port, no external network, no persistence.
exec /usr/local/bin/docker-entrypoint.sh redis-server --aclfile /data/users.acl --save '' --appendonly no --maxmemory 64mb --maxmemory-policy noeviction
