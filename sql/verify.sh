#!/usr/bin/env bash
set -eu
source /scripts/seed-config.sh
seed_sql -i /scripts/02-verify.sql
