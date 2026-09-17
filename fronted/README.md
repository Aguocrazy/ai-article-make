# 前端（fronted）

AI Article Make 的 Vue 3 客户端，目录名与仓库保持一致。完整架构、后端启动与接口说明见仓库根目录 [README.md](../README.md)。

## 技术

Vue 3 + Vite 8 + TypeScript + Axios。HTTP 封装在 `src/request.ts`：`baseURL` 为 `/api`，携带 Cookie，按后端 `BaseResponse`（`code === 0` 为成功）解包。

## 本地开发

需要先启动后端（`http://localhost:8080`）。

```sh
npm install
npm run dev
```

Vite 将 `/api` 代理到后端。当前脚手架页面仍是模板内容，登录与业务 UI 尚未接入。

```sh
npm run build    # 类型检查 + 生产构建
npm run preview  # 预览构建结果
```

推荐使用 VS Code + Vue (Official) 扩展（不要同时开 Vetur）。
