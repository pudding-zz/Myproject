# 服务器部署步骤（对齐 /opt/website）

## 架构

- `mysql` → 数据卷 `./data/mysql`
- `backend` → 仅内网 8080，主机名 `backend`
- `nginx` → 公网 80；静态目录挂载 `./frontend/dist`；`/api` 反代到 backend

## 一次性准备（Xshell）

```bash
# 若 /opt/website 已是旧骨架，建议备份后用仓库覆盖（保留 data/mysql 数据卷目录）
cd /opt
# 已有 website 且要保留数据库文件：
cp -a /opt/website/data /root/website-data-backup-$(date +%F) 2>/dev/null || true

# 拉取代码（公开仓示例；目录名保持 website）
# 若 website 已存在且不是 git 仓库，可先改名再 clone：
# mv /opt/website /opt/website.bak && git clone https://github.com/pudding-zz/MyProject.git /opt/website

cd /opt/website
git fetch origin
git checkout feature-20260806-穿书剧情底本
git pull

mkdir -p data/mysql frontend/dist logs
cp deploy/.env.example .env
nano .env   # 填写 MYSQL_* 与密码；DEEPSEEK 可先空
```

`.env` 里后端连库必须类似：

```env
MYSQL_URL=jdbc:mysql://mysql:3306/myproject?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
MYSQL_USERNAME=root
MYSQL_PASSWORD_APP=你的root密码
MYSQL_ROOT_PASSWORD=你的root密码
MYSQL_PASSWORD=你的业务用户密码
```

注意主机名是 **`mysql`**（compose 服务名），不是 `127.0.0.1`。

## 启动

```bash
cd /opt/website
docker compose up -d --build
docker compose ps
docker compose logs -f backend --tail=100
```

## 前端 dist（本机 XFTP）

1. 本机已有：`D:\code\MyProject\frontend\dist`
2. XFTP 上传到服务器：**`/opt/website/frontend/dist/`**
   - 上传后该目录下应能直接看到 `index.html`
3. 刷新浏览器：`http://101.132.119.23`

无需重启 nginx（静态只读挂载，换文件即可；若未见更新可 `docker compose restart nginx`）。

## 本地开发连库（可选）

SSH 隧道（本机）：

```bat
ssh -L 3307:127.0.0.1:3306 root@101.132.119.23
```

本地 `backend/.env`：

```env
MYSQL_URL=jdbc:mysql://127.0.0.1:3307/myproject?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

## 安全提醒

- 不要把数据库密码写进 GitHub 上的 `docker-compose.yml`
- 若密码曾出现在聊天记录中，建议在服务器上修改 MySQL 密码并同步更新 `.env`
