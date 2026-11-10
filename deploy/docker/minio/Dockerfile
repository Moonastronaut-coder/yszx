# MinIO 社区版已改为源码分发，固定最后一个官方发布标签进行开发构建。
# 仅在 Docker 构建阶段使用 Go；Windows 无需安装 Go。
FROM golang:1.24.8-alpine3.22 AS builder

ENV CGO_ENABLED=0 GOBIN=/out GOTOOLCHAIN=local
RUN go install github.com/minio/minio@RELEASE.2025-10-15T17-29-55Z
RUN go install github.com/minio/mc@RELEASE.2025-08-13T08-35-41Z

FROM alpine:3.22.2

RUN apk add --no-cache ca-certificates \
    && addgroup -g 10001 minio \
    && adduser -D -u 10001 -G minio minio \
    && mkdir -p /data /opt/minio \
    && chown 10001:10001 /data
COPY --from=builder /out/minio /out/mc /usr/local/bin/
COPY init.sh /opt/minio/init.sh

USER 10001:10001
WORKDIR /data
EXPOSE 9000 9001
CMD ["minio", "server", "/data", "--console-address", ":9001"]
