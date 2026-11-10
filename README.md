# 云枢智学

一个 Spring Boot 模块化单体后端，以及学员端、管理端两套独立 Vue 应用。基础工程已搭建，业务模块按现有需求、原型和设计逐步实现。

## 本地开发

需要 Node.js 24 LTS、JDK 21，以及已启动的 MySQL 8.4、Redis 8、MinIO。当前电脑已准备好 Docker 依赖和根目录 `.env`；项目 `.tools` 内有便携版 JDK 21、pnpm 及构建缓存，未修改系统 Java。

安装依赖（首次拉取工程或锁文件更新时）：

```powershell
node scripts/dev.mjs setup
```

在项目根目录分别打开三个终端：

```powershell
node scripts/dev.mjs backend
```

```powershell
node scripts/dev.mjs portal
```

```powershell
node scripts/dev.mjs admin
```

访问学员端 <http://127.0.0.1:5173>、管理端 <http://127.0.0.1:5174>，后端就绪检查为 <http://127.0.0.1:8080/actuator/health/readiness>。按 Ctrl+C 停止对应进程。

## 目录与说明

| 路径 | 职责 |
|---|---|
| [backend](backend/pom.xml) | Java 21、Spring Boot 4.1.2、MyBatis、会话/CSRF、统一响应与异常、依赖配置 |
| [frontend](frontend/package.json) | pnpm workspace；portal/admin 两端及 ui/shared/http 三个公共包 |
| [scripts/dev.mjs](scripts/dev.mjs) | 环境加载、本地工具选择、安装、启动与验证入口 |
| [tests/e2e](tests/e2e/foundation.spec.ts) | 两端浏览器与代理/会话验证 |
| [设计文档](docs/design/02.整体架构设计.md) | 已确认的架构、职责、数据库与接口约定 |
| [基础工程开发指南](docs/operations/基础工程开发指南.md) | 版本、启动、测试、工程边界与模块开发方式 |
| [Docker 环境说明](docs/operations/开发环境-Docker-Compose.md) | MySQL、Redis、MinIO 的运行与凭据配置 |

本阶段没有默认登录账号、业务表迁移或模拟成功的业务接口；页面中的 CSRF 握手只代表服务连接就绪，不代表已登录。MinIO 用于上传资源，Vue 构建产物仍按 Nginx 方案部署。
