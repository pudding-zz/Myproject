# 2026-08-17：前端按 roleplay-demo 整站换皮

## 变更

- 废弃望月青瓷 / 多主题壳（`MoonThemeToggle`、`stage.css` 主路径）。
- 新增 `demo-theme.css` + `AppShell`（顶栏：首页 / 穿书 / AI角色对话）。
- `/`：双入口落地页（对齐 demo `index.html`）。
- `/story`：穿书工作台（底本列表 / 手建 / 取纲）。
- `/roleplay`：双设定对话页重做样式。
- `/story/:id/edit`、`/play`：同一套视觉语言。

## 部署注意

必须重新构建前端并覆盖服务器 `frontend/dist`：

```bash
cd frontend && npm ci && npm run build
# 覆盖 /opt/website/frontend/dist 后 restart nginx
```

仅 rebuild backend **不会**更新页面样式。
