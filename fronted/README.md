# 前端（fronted）

AI Article Make 的 Vue 3 客户端。完整架构见仓库根目录 [README.md](../README.md)。

## 页面

- `/login` 登录（Session Cookie）
- `/register` 注册并自动登录
- `/` 工作台：展示当前用户；管理员可创建 / 查询 / 删除用户

## 本地开发

先启动后端 `http://localhost:8080`。

```sh
npm install
npm run dev
```

Vite 把 `/user`、`/api` 代理到后端。axios 携带 Cookie。

```sh
npm run build
npm run preview
```
