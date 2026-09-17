# AI Article Make

面向「AI 写文章」场景的全栈项目：后端提供用户体系与 OpenAPI 文档，前端用 Vue 3 对接同一套接口约定。当前已完成账号注册 / 登录 / 管理能力，文章生成业务尚未接入。

| 模块 | 路径 | 说明 |
|------|------|------|
| 后端 | `src/` | Spring Boot 3 + MyBatis-Flex，默认端口 `8080` |
| 前端 | `fronted/` | Vue 3 + Vite + TypeScript，开发端口由 Vite 分配 |
| 数据库脚本 | `sql/` | 建库建表与变更记录 |

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.5.10 | Web / AOP / Redis / Session |
| MyBatis-Flex | 1.11.1 | ORM（`spring-boot3-starter`） |
| MySQL | 8.0+ | 库名 `ai_passage_creator` |
| HikariCP | 4.0.3 | 连接池 |
| Redis | — | 缓存与 Session 存储 |
| Spring Session | — | Session 存 Redis，有效期 30 天 |
| Hutool | 5.8.32 | 工具库 |
| Knife4j | 4.5.0 | 中文 OpenAPI 文档（springdoc） |
| Maven Wrapper | 3.9.9 | 无需本机安装 Maven |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| Vite | 8 | 构建与开发代理 |
| TypeScript | 6 | 类型检查 |
| Axios | 1.20 | HTTP 客户端（`fronted/src/request.ts`） |

仓库根目录另有 `@umijs/openapi`，用于按 OpenAPI 生成 TypeScript 请求代码（需自行配置 `openapi2ts`）。

## 仓库结构

```
ai-article-make/
├── src/main/java/com/aiarticle/
│   ├── AiArticleMakeApplication.java
│   ├── common/          # BaseResponse、分页 / 删除请求、ResultUtils
│   ├── config/          # 跨域、Knife4j
│   ├── constant/        # 用户角色常量
│   ├── controller/      # User / Test / Doc
│   ├── exception/       # 业务异常、错误码、全局处理
│   ├── mapper/
│   ├── model/           # dto / entity / vo
│   └── service/
├── src/main/resources/
│   ├── application.yml          # 数据源、Redis、Session、Knife4j
│   └── application.properties   # 端口 8080、日志级别
├── sql/
│   ├── init_user.sql
│   └── CHANGELOG.md
├── fronted/             # Vue 前端（目录名即为仓库实际路径）
└── pom.xml
```

后端分层：

```
controller → service → mapper（MyBatis-Flex BaseMapper）
model/dto 请求  → entity 表映射  → vo 脱敏返回
```

## 环境要求

- JDK 21
- MySQL 8.0+
- Redis（本机默认 `localhost:6379`，无密码）
- Node.js `^22.18.0` 或 `>=24.12.0`（仅跑前端时需要）

## 快速开始

### 1. 初始化数据库

在 MySQL 中执行：

```bash
mysql -u root -p < sql/init_user.sql
```

脚本会创建库 `ai_passage_creator`、表 `user`，并插入测试账号。变更履历见 [`sql/CHANGELOG.md`](sql/CHANGELOG.md)。

预置账号（明文密码均为 `12345678`）：

| 账号 | 角色 |
|------|------|
| `admin` | 管理员 |
| `user` | 普通用户 |
| `test` | 普通用户 |

### 2. 配置后端

编辑 `src/main/resources/application.yml` 中的 MySQL 账号密码；Redis 默认本机即可。不要把真实密码提交进仓库。

应用通过实体 `User` 使用**雪花 ID**（`KeyType.Generator` + `snowFlakeId`），逻辑删除字段 `isDelete`。表字段为驼峰命名，MyBatis-Flex 关闭了下划线转驼峰（`map-underscore-to-camel-case: false`）。

### 3. 启动后端

```bash
./mvnw spring-boot:run
# 或
./mvnw clean package -DskipTests
java -jar target/ai-article-make-0.0.1-SNAPSHOT.jar
```

服务地址：`http://localhost:8080`（无 `context-path`）。

### 4. 启动前端

```bash
cd fronted
npm install
npm run dev
```

Vite 把以 `/api` 开头的请求代理到 `http://localhost:8080`。前端 axios 的 `baseURL` 为 `/api` 且 `withCredentials: true`，与后端 CORS（允许 Cookie、`allowedOriginPatterns: *`）配合使用。

注意：用户接口实际路径是 `/user/...`，不是 `/api/user/...`。调试用户模块可直接打后端，或把代理 rewrite / `baseURL` 按实际路径调整。测试接口本身挂在 `/api/test` 下，可走当前代理。

## 接口文档

启动后端后：

- Knife4j：http://localhost:8080/doc.html
- 带 `/api` 前缀的入口（重定向到上面）：http://localhost:8080/api/doc.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs/default

## 用户模块

| 说明 | 方法 | 路径 | 权限 |
|------|------|------|------|
| 注册 | POST | `/user/register` | 公开 |
| 登录 | POST | `/user/login` | 公开 |
| 当前用户 | GET | `/user/get/login` | 登录 |
| 注销 | POST | `/user/logout` | 登录 |
| 创建用户 | POST | `/user/add` | 管理员 |
| 删除用户 | POST | `/user/delete` | 管理员（逻辑删除） |
| 更新用户 | POST | `/user/update` | 管理员 |
| 分页查询 | POST | `/user/list/page/vo` | 管理员（`pageSize` 最大 50） |

注册规则：账号 4～256 位，密码 8～512 位，需填写 `checkPassword` 且两次一致。查重先查库，插入时捕获 `uk_userAccount` 唯一索引冲突作为兜底。新密码存为 `随机盐$MD5(明文+盐+pepper)`，每人一盐以防彩虹表；种子账号仍兼容旧格式 `MD5(明文+yupi)`。登录态写入 Session，Spring Session 存 Redis，Cookie / Session 超时均为 30 天。管理员接口在 Controller 内校验 `userRole == admin`。

其它示例接口：`GET /api/test/hello`、`GET /api/test/user/{id}`（演示 Knife4j 与 Hutool，不走统一 `BaseResponse`）。

## 统一响应与错误码

业务接口返回 `BaseResponse<T>`：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40001 | 请求数据为空 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40300 | 禁止访问 |
| 40400 | 请求数据不存在 |
| 50000 | 系统内部异常 |
| 50001 | 操作失败 |

`BusinessException` 与未捕获 `RuntimeException` 由 `GlobalExceptionHandler` 转成上述结构。Controller 侧常用：

```java
return ResultUtils.success(data);
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR, "说明");
throw new BusinessException(ErrorCode.OPERATION_ERROR, "说明");
```

分页请求基类 `PageRequest`：`current` 从 1 起，默认 `pageSize = 10`，`sortOrder` 为 `ascend` / `descend`。

## 当前范围与后续

已具备：用户表、Session 登录、管理员 CRUD、跨域、接口文档、Vue 请求封装。

尚未实现：文章生成、模型调用、前端登录页与业务页面。扩展数据库时新增编号脚本并在 `sql/CHANGELOG.md` 登记，不要改已执行过的 SQL 文件。
