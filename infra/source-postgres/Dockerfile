FROM postgres:17-alpine
RUN apk add --no-cache openssl
COPY source-entrypoint.sh /usr/local/bin/source-entrypoint.sh
COPY init.sh /docker-entrypoint-initdb.d/10-business.sh
RUN chmod +x /usr/local/bin/source-entrypoint.sh /docker-entrypoint-initdb.d/10-business.sh
ENTRYPOINT ["/usr/local/bin/source-entrypoint.sh"]
CMD ["postgres", "-c", "ssl=on", "-c", "ssl_cert_file=/var/lib/postgresql/tls/server.crt", "-c", "ssl_key_file=/var/lib/postgresql/tls/server.key"]
