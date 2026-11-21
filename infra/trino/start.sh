#!/usr/bin/env bash
set -euo pipefail
umask 077
java -Xmx64m -cp /opt/atlas Bootstrap
exec /usr/lib/trino/bin/launcher run --etc-dir /etc/trino
