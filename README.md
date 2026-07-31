# ShareHub（拼友）

一个面向小规模合租场景的管理平台，用于统一管理共享账号、梯子订阅、成员租期和节点状态。项目采用前后端分离架构，优先保障登录、注册审批、数据权限和敏感信息安全。

> 当前处于早期开发阶段。登录、验证码、注册审批、注册开关和基础安全能力已接入后端；共享项目、租户、订单及续费等业务页面目前主要是交互原型，尚未全部接入持久化接口。

## 已有功能

- 登录验证码与 Redis 登录限流
- 管理员审批注册申请
- 管理员开关公开注册入口和注册接口
- Spring Security 会话认证与接口权限控制
- Redis / Spring Session 会话存储
- 密码哈希、敏感字段 AES-GCM 加密及接口脱敏基础设施
- 安全审计日志
- PostgreSQL 与 MySQL 双数据库支持
- Flyway 自动维护数据库结构
- 合租工作台、共享项目、用户、订单和系统设置界面
- 梯子订阅可关联 Komari 等探针状态页；探针本身不作为出租项目

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 前端 | React 19、TypeScript、Vinext / Vite |
| 后端 | Java 17、Spring Boot、Spring Security |
| 数据访问 | MyBatis-Plus、Flyway |
| 数据库 | PostgreSQL 17 或 MySQL 8.4 |
| 缓存与会话 | Redis 7、Spring Session |
| 部署 | Docker Compose |

## 项目结构

```text
sharehub/
├─ app/                         前端页面和样式
├─ backend/                     Spring Boot 后端
│  └─ src/main/resources/
│     └─ db/migration/          PostgreSQL / MySQL 迁移脚本
├─ public/                      前端静态资源
├─ tests/                       前端构建与渲染测试
├─ docker-compose.yml           API 与 Redis 公共配置
├─ docker-compose.postgresql.yml
├─ docker-compose.mysql.yml
├─ .env.example                 环境变量模板
└─ SECURITY.md                  安全策略和部署检查项
```

## 环境要求

- Node.js 22.13 或更高版本
- Java 17
- Maven 3.9+
- Redis 7
- PostgreSQL 17 或 MySQL 8.4

如果使用 Docker Compose，只需准备 Docker Engine 与 Compose 插件。

## 本地开发

### 1. 准备配置

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

至少替换以下配置，禁止直接使用模板值：

- `DB_PASSWORD`
- `REDIS_PASSWORD`（本机 Redis 无密码时可暂时留空）
- `CAPTCHA_SECRET`
- `IP_HASH_SALT`
- `SHAREHUB_MASTER_KEY`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

生成 32 字节主密钥可使用：

```bash
openssl rand -base64 32
```

`.env` 包含真实凭据，已被 Git 忽略，不要提交到仓库。

### 2. 启动基础服务

默认端口：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8787`
- Redis：`127.0.0.1:6379`

请先启动 Redis 和选定的数据库，并创建名为 `sharehub` 的数据库及对应用户。数据库表由 Flyway 在后端启动时自动创建或升级。

### 3. 启动后端

PostgreSQL：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:SPRING_PROFILES_ACTIVE = 'postgresql'
mvn -s C:\Users\Admin\.m2\settings.xml -f backend/pom.xml spring-boot:run
```

MySQL：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:SPRING_PROFILES_ACTIVE = 'mysql'
mvn -s C:\Users\Admin\.m2\settings.xml -f backend/pom.xml spring-boot:run
```

PowerShell 不会自动读取 `.env`。本地调试时需要把其中的变量导入当前终端，或在 IDE 的运行配置中设置。后端的详细说明见 [`backend/README.md`](backend/README.md)。

### 4. 启动前端

```bash
npm install
npm run dev
```

当前前端开发环境固定请求 `http://localhost:8787/api`，因此前后端应分别使用 3000 和 8787 端口。

## Docker 部署

### PostgreSQL

```bash
docker compose -f docker-compose.yml -f docker-compose.postgresql.yml up -d --build
```

### MySQL

```bash
docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d --build
```

这两组 Compose 配置当前负责后端 API、Redis 和数据库。前端尚未包含在 Compose 中，生产环境需要单独构建和托管，并通过反向代理连接 API。

查看服务状态和日志：

```bash
docker compose -f docker-compose.yml -f docker-compose.postgresql.yml ps
docker compose -f docker-compose.yml -f docker-compose.postgresql.yml logs -f api
```

生产环境不要直接暴露数据库和 Redis 端口。建议由 Nginx 或 Caddy 统一提供 HTTPS，并设置：

```env
APP_ORIGIN=https://你的域名
COOKIE_SECURE=true
TRUST_PROXY=1
```

## 常用检查

前端：

```bash
npm run lint
npm test
```

后端：

```powershell
mvn -s C:\Users\Admin\.m2\settings.xml -f backend/pom.xml compile
```

检查 Compose 配置：

```bash
docker compose -f docker-compose.yml -f docker-compose.postgresql.yml config
docker compose -f docker-compose.yml -f docker-compose.mysql.yml config
```

## 数据库迁移

Flyway 会根据 `SPRING_PROFILES_ACTIVE` 自动选择迁移目录：

- PostgreSQL：`backend/src/main/resources/db/migration/postgresql`
- MySQL：`backend/src/main/resources/db/migration/mysql`

已在环境中执行过的迁移文件不要修改。数据库结构需要变化时，应新增更高版本的迁移文件，例如 `V3__add_subscription_expiry.sql`。

## 安全提示

- 首次启动前必须更换管理员密码和所有示例密钥。
- 不要把 `.env`、数据库备份、运行日志或构建产物提交到 Git。
- Redis、PostgreSQL 和 MySQL 只应在可信内网中访问。
- 生产环境必须使用 HTTPS，并启用安全 Cookie。
- 不要只依赖前端隐藏按钮；所有权限判断必须由后端执行。
- 定期备份数据库，并验证备份能够恢复。

更完整的安全说明见 [`SECURITY.md`](SECURITY.md)。

## 当前待完善

- 将共享项目、租户、套餐、订单和续费页面接入真实后端接口
- 按服务类型独立管理租期，支持用户只续租部分服务
- 梯子订阅与探针链接的授权、脱敏和过期控制
- 管理端用户状态、角色与数据范围管理
- 前端生产构建、反向代理及完整 Docker 部署
- 自动化后端测试和端到端测试

## License

当前仓库尚未声明开源许可证。未经授权，请勿将代码视为可自由复制或再分发的软件。
