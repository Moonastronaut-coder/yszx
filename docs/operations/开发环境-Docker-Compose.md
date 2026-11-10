# 云枢智学开发环境：Docker Compose

日期：2026-10-30。用户确认上传资源使用 MinIO，Vue 构建产物仍由 Nginx 部署。本文件覆盖单机开发基础设施，前后端应用暂在 Windows 上运行。

## 1. 文件与服务

- 编排：[compose.dev.yaml](../../deploy/compose/compose.dev.yaml)。
- 环境变量：[.env.example](../../.env.example)，复制成根目录 `.env` 后填写。
- MinIO 镜像：[Dockerfile](../../deploy/docker/minio/Dockerfile)；初始化：[init.sh](../../deploy/docker/minio/init.sh)。

| 服务 | 固定版本 | Windows 访问地址 | 同一 Compose 网络的地址 |
|---|---|---|---|
| MySQL | 8.4.13 | `127.0.0.1:13306` | `mysql:3306` |
| Redis | 8.4.8-alpine3.22 | `127.0.0.1:16379` | `redis:6379` |
| MinIO S3 API | RELEASE.2025-10-15T17-29-55Z，源码构建 | `http://127.0.0.1:19000` | `http://minio:9000` |
| MinIO 控制台 | 与服务端一同构建 | `http://127.0.0.1:19001` | `http://minio:9001` |
| minio-init | 同一个本地镜像，含固定版本 mc | 一次性任务，无映射端口 | 等待 MinIO 健康后创建私有桶与应用账号 |

端口可在 `.env` 中调整，所有发布端口只绑定 `127.0.0.1`。Compose 默认创建独立项目网络以及 MySQL、Redis、MinIO 三个命名数据卷，不复用本机 MySQL 8.0 的数据目录。

## 2. MinIO 镜像来源

MinIO 官方社区仓库于 2026-04-25 归档，官方说明社区版本改为源码分发。最后一个发布标签为 `RELEASE.2025-10-15T17-29-55Z`，其发布说明要求容器环境从源码构建。本项目因此使用多阶段 Dockerfile，在 Go 构建容器中编译固定标签的服务端和 `mc`，Windows 无需另装 Go。首次构建需要访问 Docker Hub、Go 模块代理及其校验服务，耗时取决于网络与构建资源。

构建工具镜像固定为 `golang:1.24.8-alpine3.22`，运行镜像固定为 `alpine:3.22.2`；镜像尚未锁定 digest。2026-10-30 用户另行授权启动后，已完成本机拉取、构建和启动验证，见第 7 节。社区仓库已停止维护，本配置定位开发环境；生产版本来源、维护与恢复方案须另行确定。

官方依据：[仓库现状](https://github.com/minio/minio)、[最后发布说明](https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z)、[固定 mc 标签源码](https://github.com/minio/mc/tree/RELEASE.2025-08-13T08-35-41Z)。

其他镜像标签参考：[MySQL 官方镜像](https://hub.docker.com/_/mysql)、[Redis 官方镜像](https://hub.docker.com/_/redis)。

## 3. 首次准备与启动

在项目根目录 `D:\云枢智学` 打开 PowerShell，确认 Docker Desktop 已启动并使用 Linux 容器。

```powershell
Copy-Item -LiteralPath .env.example -Destination .env
```

仅首次复制；已有 `.env` 时直接编辑它，避免覆盖原凭据。填写五个密码变量；MinIO 密码至少 8 位，建议所有密码使用不同的至少 24 位随机值。变量值包含 `$`、`#` 时，用单引号包住整个值。管理员账号和应用账号必须不同。真实 `.env` 不提交 Git；开发配置通过环境变量传入凭据，容器检查信息中可见，生产密钥管理另行设计。

先验证配置，再构建本地 MinIO 镜像，最后启动依赖：

```powershell
docker compose --env-file .env -f deploy/compose/compose.dev.yaml config --quiet
docker compose --env-file .env -f deploy/compose/compose.dev.yaml build minio
docker compose --env-file .env -f deploy/compose/compose.dev.yaml up -d
docker compose --env-file .env -f deploy/compose/compose.dev.yaml ps -a
```

预期：MySQL、Redis、MinIO 显示 `healthy`，`minio-init` 为 `Exited (0)`。启动命令返回不代表初始化已经完成，应检查状态；如初始化失败，查看日志：

```powershell
docker compose --env-file .env -f deploy/compose/compose.dev.yaml logs --tail 100 minio-init
```

首次生成时仅进行了静态/Compose 解析验证。随后用户明确要求启动，已执行以上镜像构建和启动步骤，实际结果见第 7 节。

## 4. MySQL 与 Redis

- MySQL 首次空数据卷初始化时创建项目数据库与应用用户。容器默认时区为 UTC，字符集为 `utf8mb4`、排序规则为 `utf8mb4_0900_as_ci`；应用连接仍须明确设置 UTC，业务日按 `Asia/Shanghai` 计算。
- `MYSQL_USER` 只对项目库有权限；管理员账号供初始化和受控维护。后续生产应进一步区分迁移账号与运行账号。
- 当前 `scripts/sql/schema.sql` 是审阅脚本，本 Compose 不自动执行它，也不注入默认超级管理员密码、业务表或样例数据。后续将审定结构纳入 Flyway，再处理业务初始化。
- MySQL 的数据库名、账号和密码初始化变量只在空数据目录生效。已有数据卷时仅修改 `.env` 不会更改已存在账号，应通过受控 SQL 修改并同步连接配置。
- Redis 启用密码、AOF 和 `appendfsync everysec`，开发内存上限默认 `256mb`、淘汰策略为 `noeviction`。达到上限可能拒绝写入，需控制普通缓存规模并监控；AOF 不替代备份，故障时仍可能损失近期写入，敏感操作恢复策略由业务实现保证。

## 5. MinIO 存储与业务连接

初始化任务创建默认私有桶 `yunshu-assets`，配置只允许访问该桶的应用账号。头像、Logo、默认头像等资源可按对象前缀分类，具体对象键由后续文件适配实现；不保存 Markdown 导入原文档。桶不开放匿名读取，也不配置浏览器直传 CORS；上传和读取继续通过已设计的后端文件接口授权，公开 Logo/默认头像由其公开接口按当前用途提供。

| 后端配置用途 | Windows 开发时的值 |
|---|---|
| 数据库 | `.env` 中的库名、`MYSQL_USER`、`MYSQL_PASSWORD`，主机 `127.0.0.1`，端口 `13306` |
| Redis | 主机 `127.0.0.1`，端口 `16379`，密码取 `REDIS_PASSWORD` |
| 文件适配标识 | 后续适配约定为 `minio`；复用 `file_asset` 存储元信息，不新增表字段 |
| S3 endpoint | `http://127.0.0.1:19000` |
| S3 region / 访问方式 | `us-east-1` / path-style |
| bucket | `MINIO_BUCKET` |
| access key / secret key | `MINIO_APP_ACCESS_KEY` / `MINIO_APP_SECRET_KEY`，后端使用应用账号 |
| 控制台登录 | 浏览器打开 `http://127.0.0.1:19001`，使用 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` |

后端容器化后改用服务名连接。若后续由后端生成供浏览器使用的签名 URL，签名 endpoint 必须是浏览器实际能访问的地址；浏览器无法解析 Compose 内部服务名 `minio`，且签名生成后不能简单替换主机地址。

首次初始化和后续显式重跑会创建或更新固定策略及所指定应用用户的密码，并将指定桶设为私有。修改 MinIO 应用密码/账号或桶名后，先检查目标环境，再显式执行：

```powershell
docker compose --env-file .env -f deploy/compose/compose.dev.yaml run --rm minio-init
```

更换账号或桶名不会自动迁移旧对象、清理旧用户或旧策略绑定；需按实际迁移方案处理。

## 6. 停止、持久化与后续部署

```powershell
docker compose --env-file .env -f deploy/compose/compose.dev.yaml down
```

普通 `down` 保留命名数据卷，重建容器后可继续使用。`down --volumes` 会删除数据卷，本说明不将它作为日常停止方式。数据卷的持久化不等于备份，重要数据库和文件需要单独导出、备份并验证恢复。

本 Compose 仅启动基础设施。后续已搭建可运行的后端与两套 Vue 基础工程，开发启动方式见[基础工程开发指南](基础工程开发指南.md)，不在本 Compose 中重复定义应用。Vue 的 HTML/JS/CSS 构建产物仍由 Nginx 部署；生产域名、HTTPS、存储维护及容量预算按剩余部署输入确定。

## 7. 实际启动验证（2026-10-30）

用户明确授权启动后，已在本机 Docker Desktop 的 Linux 引擎构建 MinIO 镜像、拉取 MySQL/Redis 镜像并启动项目。首次创建根目录 `.env`，五个密码使用独立随机值；未在日志或聊天中输出，后续启动请保留该文件。

| 检查项 | 实际结果 |
|---|---|
| MySQL | `healthy`，监听 `127.0.0.1:13306` |
| Redis | `healthy`，监听 `127.0.0.1:16379`；健康检查参数已修正为 `redis-cli -h` |
| MinIO | `healthy`，就绪接口返回 HTTP 200 |
| MinIO 控制台 | `http://127.0.0.1:19001` 返回 HTTP 200 |
| MinIO 初始化 | `minio-init` 退出码为 0；私有桶 `yunshu-assets` 和应用账号初始化成功 |
| 私有桶匿名访问 | HTTP 403，符合私有访问配置 |

以后在项目根目录、Docker Desktop 已启动时，可以直接执行以下命令，无需重新复制 `.env` 或重复构建镜像：

```powershell
docker compose --env-file .env -f deploy/compose/compose.dev.yaml up -d
docker compose --env-file .env -f deploy/compose/compose.dev.yaml ps -a
```

本次未创建业务表、业务账号初始数据或启动前后端业务应用。
