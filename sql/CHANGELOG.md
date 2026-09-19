# SQL 执行历史

> 记录项目数据库变更的执行历史，按时间倒序排列。每次对数据库结构的变更（建表、加字段、改索引等）都应在此登记。

| 序号 | 日期 | 脚本文件 | 说明 | 状态 |
|------|------|----------|------|------|
| 3 | 2026-09-19 | [003_create_article.sql](003_create_article.sql) | 创建 `article` 文章表（任务 ID、选题、正文、配图、生成状态） | ✅ 已执行 |
| 2 | 2026-09-16 | [init_user.sql](init_user.sql) | 创建 `user` 用户表（含唯一索引 uk_userAccount、普通索引 idx_userName），插入 3 条测试数据（admin/user/test，密码均为 12345678） | ✅ 已执行 |
| 1 | 2026-09-10 | （命令行） | 创建数据库 `ai_passage_creator`（utf8mb4_unicode_ci） | ✅ 已执行 |

---

## 详细变更记录

### #3 - 003_create_article.sql（2026-09-19）

**变更内容：** 创建 `article` 表，用于存储选题生成任务与成品。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| taskId | varchar(64) | 任务 UUID，唯一索引 |
| userId | bigint | 用户 ID，索引 |
| topic | varchar(500) | 选题 |
| mainTitle | varchar(200) | 主标题 |
| subTitle | varchar(300) | 副标题 |
| outline | json | 大纲 |
| content | text | 正文 Markdown |
| fullContent | text | 含配图的完整 Markdown |
| coverImage | varchar(512) | 封面图 URL |
| images | json | 配图列表 |
| status | varchar(20) | PENDING / PROCESSING / COMPLETED / FAILED |
| errorMessage | text | 失败原因 |
| createTime | datetime | 创建时间 |
| completedTime | datetime | 完成时间 |
| updateTime | datetime | 更新时间 |
| isDelete | tinyint | 逻辑删除 |

**回滚脚本：**
```sql
DROP TABLE IF EXISTS article;
```

### #2 - init_user.sql（2026-09-16）

**变更内容：**

1. 创建 `user` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| userAccount | varchar(256) | 账号，唯一索引 |
| userPassword | varchar(512) | 密码（MD5 + 盐值加密存储） |
| userName | varchar(256) | 用户昵称，普通索引 |
| userAvatar | varchar(1024) | 用户头像 |
| userProfile | varchar(512) | 用户简介 |
| userRole | varchar(256) | 角色：user/admin，默认 user |
| editTime | datetime | 编辑时间 |
| createTime | datetime | 创建时间 |
| updateTime | datetime | 更新时间（自动更新） |
| isDelete | tinyint | 逻辑删除，默认 0 |

2. 插入测试数据 3 条（admin / user / test，明文密码均为 `12345678`）

**回滚脚本（如需撤销）：**
```sql
DROP TABLE IF EXISTS user;
```

### #1 - 创建数据库（2026-09-10）

**变更内容：** 创建数据库 `ai_passage_creator`，字符集 utf8mb4

```sql
CREATE DATABASE IF NOT EXISTS ai_passage_creator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 维护规范

1. **新增变更**：新建编号脚本（如 `002_add_article_table.sql`），并在上方表格登记
2. **命名建议**：`序号_操作_对象.sql`，如 `003_add_column_user_phone.sql`
3. **禁止修改**：已执行过的脚本文件不再改动，需要变更时新增脚本
4. **回滚预案**：重要变更应在详细记录中附带回滚 SQL