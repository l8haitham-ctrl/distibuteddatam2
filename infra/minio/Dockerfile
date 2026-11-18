# Build the official, source-only community release (AGPL-3.0).
FROM golang:1.24.8-alpine AS build
ARG MINIO_RELEASE=RELEASE.2025-10-15T17-29-55Z
ENV CGO_ENABLED=0 GOBIN=/out GOMAXPROCS=2
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go install -v -p=2 github.com/minio/minio@${MINIO_RELEASE} && \
    cp "$(go list -m -f '{{.Dir}}' github.com/minio/minio@${MINIO_RELEASE})/LICENSE" /out/LICENSE

FROM alpine:3.23
RUN apk add --no-cache ca-certificates curl && \
    addgroup -g 10001 -S minio && adduser -u 10001 -S -G minio minio && \
    mkdir /data && chown minio:minio /data
COPY --from=build /out/minio /usr/local/bin/minio
COPY --from=build /out/LICENSE /usr/share/licenses/minio/LICENSE
USER 10001:10001
EXPOSE 9000 9001
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
