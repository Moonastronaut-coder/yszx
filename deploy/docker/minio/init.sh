#!/bin/sh
set -eu

# 桶名限制为开发环境约定的 3..63 位小写字母、数字和连字符。
if ! printf '%s' "$MINIO_BUCKET" | grep -Eq '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$'; then
  echo 'MINIO_BUCKET 必须为 3..63 位小写字母、数字和连字符，且首尾不能为连字符。' >&2
  exit 1
fi
if [ "$MINIO_ROOT_USER" = "$MINIO_APP_ACCESS_KEY" ]; then
  echo 'MinIO 应用账号必须与管理员账号不同。' >&2
  exit 1
fi

mc alias set bootstrap http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
mc mb --ignore-existing "bootstrap/$MINIO_BUCKET" >/dev/null
mc anonymous set none "bootstrap/$MINIO_BUCKET" >/dev/null

# 仅允许访问本项目桶；读取仍由后端文件接口按所有权/用途授权。
cat > /tmp/app-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketMultipartUploads"],
      "Resource": ["arn:aws:s3:::$MINIO_BUCKET"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:AbortMultipartUpload", "s3:ListMultipartUploadParts"],
      "Resource": ["arn:aws:s3:::$MINIO_BUCKET/*"]
    }
  ]
}
EOF

mc admin policy create bootstrap yunshu-assets-rw /tmp/app-policy.json >/dev/null
mc admin user add bootstrap "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY" >/dev/null
mc admin policy attach bootstrap yunshu-assets-rw --user "$MINIO_APP_ACCESS_KEY" >/dev/null
mc alias set app http://minio:9000 "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY" >/dev/null
mc ls "app/$MINIO_BUCKET" >/dev/null
echo "MinIO 初始化完成：私有桶 $MINIO_BUCKET、应用账号及桶内读写策略已就绪。"
