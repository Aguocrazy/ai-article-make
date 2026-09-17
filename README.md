# AI Article Make

AI 文章生成平台后端服务。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.5.10 | 基础框架 |
| MyBatis-Flex | 1.11.1 | ORM 框架（spring-boot3-starter） |
| MySQL | 8.0.x | 数据库（ai_passage_creator） |
| HikariCP | 4.0.3 | 数据库连接池 |
| Redis | — | 缓存 / Session 存储 |
| Spring Session | — | 分布式 Session（Redis 存储，30 天） |
| Spring AOP | — | 面向切面编程 |
| Lombok | — | 简化样板代码 |
| Hutool | 5.8.32 | Java 工具库 |
| Knife4j | 4.5.0 | 接口文档（基于 springdoc-openapi，中文界面） |
| Maven Wrapper | 3.9.9 | 构建工具（无需本地安装 Maven） |

## 项目结构

```
src/main/java/com/aiarticle/
├── AiArticleMakeApplication.java   # 启动类（@EnableAspectJAutoProxy 启用 AOP）
├── common/                      # 通用类
│   ├── BaseResponse.java        # 通用响应类
│   ├── DeleteRequest.java       # 删除请求
│   ├── PageRequest.java         # 分页请求
│   └── ResultUtils.java         # 响应工具类
├── exception/                   # 异常处理
│   ├── BusinessException.java   # 业务异常
│   ├── ErrorCode.java           # 错误码枚举
│   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   └── ThrowUtils.java          # 异常工具类
├── config/                      # 配置类
│   ├── CorsConfig.java          # 跨域配置
│   └── Knife4jConfig.java       # 接口文档配置
├── constant/                    # 常量
│   └── UserConstant.java        # 用户角色常量
├── mapper/                      # 数据访问层（MyBatis-Flex BaseMapper）
│   └── UserMapper.java
├── model/
│   ├── dto/user/                # 请求 DTO（Register/Login/Add/Update/Query）
│   ├── entity/                  # 数据库实体（User，雪花 ID + 逻辑删除）
│   └── vo/                      # 视图对象（LoginUserVO / UserVO，脱敏）
├── service/                     # 业务逻辑
│   ├── UserService.java
│   └── impl/UserServiceImpl.java
└── controller/                  # 控制器
    ├── UserController.java      # 用户模块（注册/登录/注销/管理）
    ├── TestController.java      # 测试接口
    └── DocController.java       # 文档入口（/api/doc.html 重定向）
```

## 用户模块接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户注册 | POST | /user/register | 账号密码注册 |
| 用户登录 | POST | /user/login | Session 认证 |
| 获取登录用户 | GET | /user/get/login | 获取当前用户信息 |
| 用户注销 | POST | /user/logout | 退出登录 |
| 创建用户 | POST | /user/add | 管理员专用 |
| 删除用户 | POST | /user/delete | 管理员专用（逻辑删除） |
| 更新用户 | POST | /user/update | 管理员专用 |
| 分页查询用户 | POST | /user/list/page/vo | 管理员专用 |

- 密码加密：`md5(密码 + 盐值 yupi)`
- 登录态：Session（Spring Session 存 Redis，30 天）
- 数据库变更历史见 `sql/CHANGELOG.md`

## 核心设计

### 统一响应格式

所有接口统一返回 `BaseResponse<T>`：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

| code | 说明 |
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

### 使用示例

```java
// Controller 返回成功响应
@GetMapping("/user")
public BaseResponse<User> getUser() {
    return ResultUtils.success(user);
}

// 业务校验：条件成立自动抛出业务异常，由全局异常处理器兜底
ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

// 主动抛出业务异常
throw new BusinessException(ErrorCode.OPERATION_ERROR, "自定义错误信息");
```

## 环境要求

- JDK 21
- MySQL 8.0+（数据库 `ai_passage_creator`）
- Redis

## 快速开始

```bash
# 1. 配置数据库与 Redis（src/main/resources/application.yml）
#    spring.datasource.url / username / password
#    spring.data.redis.host / port

# 2. 编译
./mvnw clean compile

# 3. 启动
./mvnw spring-boot:run
# 或打包运行
./mvnw clean package -DskipTests
java -jar target/ai-article-make-0.0.1-SNAPSHOT.jar
```

## 接口文档

启动后访问：

- **Knife4j 文档**：http://localhost:8080/doc.html
- **文档入口（/api 前缀）**：http://localhost:8080/api/doc.html
- **OpenAPI JSON**：http://localhost:8080/v3/api-docs/default