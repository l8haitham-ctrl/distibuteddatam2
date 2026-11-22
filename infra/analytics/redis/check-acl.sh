#!/bin/sh
set -eu
export REDISCLI_AUTH="$REDIS_READER_PASSWORD"
read_value=$(redis-cli --user atlas_reader --no-auth-warning HGET analytics:customer_segments:5 segment)
test "$read_value" = vip
denied=$(redis-cli --user atlas_reader --no-auth-warning SET analytics:customer_segments:acl_probe forbidden)
case "$denied" in NOPERM*) ;; *) echo 'Reader write was not denied'; exit 1 ;; esac
outside=$(redis-cli --user atlas_reader --no-auth-warning GET unrelated:key)
case "$outside" in NOPERM*) ;; *) echo 'Out-of-scope read was not denied'; exit 1 ;; esac
unauth=$(unset REDISCLI_AUTH; redis-cli --no-auth-warning PING)
case "$unauth" in NOAUTH*) ;; *) echo 'Unauthenticated access was not denied'; exit 1 ;; esac
echo 'Reader: allowed fixture reads; denied writes, other keys and anonymous access.'
