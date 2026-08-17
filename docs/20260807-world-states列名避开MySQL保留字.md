# 2026-08-07：world_states 列名避开 MySQL 保留字

## 背景

服务器部署后，前端首页调用 `/api/story-bases` 返回 `internal error`，页面刷新无效。

根因：Hibernate 建表时使用列名 `current_time`，与 MySQL 保留字冲突，DDL 失败，`world_states` 表未正确创建；列表接口在组装响应时查询该表，触发未捕获异常，统一返回 `internal error`。

前端 API 路径、Nginx `/api` 反代、Vite 本地代理本身对接正确；报错文案里的「请确认 Vite 代理」仅为本地开发提示，不能当作线上根因。

## 修改内容

| 位置 | 变更 |
|------|------|
| `backend/.../entity/WorldStateEntity.java` | `@Column(name = "current_time")` 改为 `@Column(name = "world_time")` |
| Java 字段名 | 仍为 `currentTime`（仅改库列名，业务代码无需改调用） |

提交：`b890285` — `fix: world_states 列名避开 MySQL 保留字 current_time`

## 部署注意

1. 本修复曾只在本地，未推到 `origin/develop`，服务器 `git pull` 拉不到。
2. 若库中已有半残表/错误列，pull 并重建 backend 镜像后，必要时清理相关表再启，让 `ddl-auto` 按新列名重建。
3. 验证：`curl http://127.0.0.1/api/story-bases` 应返回 `code: 0`（可为空列表）。

## 影响范围

- 仅数据库列名映射；前后端 JSON 字段仍为 `currentTime`（DTO），无需改前端。
