#!/bin/sh
set -eu
# Idempotent fixture: deterministic HSET on our own 1,000 keys, no FLUSHDB.
i=1
while [ "$i" -le 1000 ]; do
  segment=standard
  if [ $((i % 5)) -eq 0 ]; then segment=vip; fi
  redis-cli -e -h redis-source --user atlas_admin --no-auth-warning HSET "analytics:customer_segments:$i" customer_id "$i" segment "$segment" loyalty_points "$((i * 10))" > /dev/null
  i=$((i + 1))
done
echo 'Seeded 1000 customer segments (200 vip, 800 standard).'
