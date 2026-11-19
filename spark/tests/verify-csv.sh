#!/usr/bin/env bash
set -euo pipefail
mkdir -p /tmp/csv-check-classes
javac --release 21 -cp '/opt/spark/jars/*:/opt/atlas/atlas-ingestion.jar' -d /tmp/csv-check-classes /checks/CsvContractSmoke.java
jar --create --file /tmp/csv-check.jar -C /tmp/csv-check-classes .
exec /opt/spark/bin/spark-submit --master 'local[1]' --driver-memory 512m --conf spark.ui.enabled=false --conf spark.task.maxFailures=1 --jars /opt/atlas/atlas-ingestion.jar --class CsvContractSmoke /tmp/csv-check.jar
