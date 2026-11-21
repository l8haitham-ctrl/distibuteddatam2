#!/usr/bin/env bash
set -euo pipefail
umask 077
xml_escape() { printf '%s' "$1" | sed 's/\&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g'; }
metastore_password_xml="$(xml_escape "$METASTORE_PASSWORD")"
s3_key_xml="$(xml_escape "$TRINO_S3_ACCESS_KEY")"
s3_secret_xml="$(xml_escape "$TRINO_S3_SECRET_KEY")"
cat > /opt/hive/conf/hive-site.xml <<XML
<configuration>
<property><name>javax.jdo.option.ConnectionURL</name><value>jdbc:postgresql://metadata:5432/metastore</value></property>
<property><name>javax.jdo.option.ConnectionDriverName</name><value>org.postgresql.Driver</value></property>
<property><name>javax.jdo.option.ConnectionUserName</name><value>metastore</value></property>
<property><name>javax.jdo.option.ConnectionPassword</name><value>${metastore_password_xml}</value></property>
<property><name>hive.metastore.warehouse.dir</name><value>file:/tmp/warehouse</value></property>
<property><name>hive.metastore.schema.verification</name><value>true</value></property>
<property><name>fs.s3a.endpoint</name><value>http://minio:9000</value></property>
<property><name>fs.s3.impl</name><value>org.apache.hadoop.fs.s3a.S3AFileSystem</value></property>
<property><name>fs.s3a.impl</name><value>org.apache.hadoop.fs.s3a.S3AFileSystem</value></property>
<property><name>fs.s3a.path.style.access</name><value>true</value></property>
<property><name>fs.s3a.connection.ssl.enabled</name><value>false</value></property>
<property><name>fs.s3a.access.key</name><value>${s3_key_xml}</value></property>
<property><name>fs.s3a.secret.key</name><value>${s3_secret_xml}</value></property>
<property><name>fs.s3a.aws.credentials.provider</name><value>org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider</value></property>
</configuration>
XML
chmod 600 /opt/hive/conf/hive-site.xml
export HADOOP_HEAPSIZE=512
/opt/hive/bin/schematool -dbType postgres -info >/tmp/schema-info.log 2>&1 || /opt/hive/bin/schematool -dbType postgres -initSchema
exec /opt/hive/bin/hive --service metastore -p 9083
