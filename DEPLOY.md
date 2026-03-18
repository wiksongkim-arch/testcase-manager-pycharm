# TestCase Manager 部署指南

## 快速开始

### 使用 Docker Compose（推荐）

1. **克隆仓库**
   ```bash
   git clone https://github.com/your-org/testcase-manager.git
   cd testcase-manager
   ```

2. **配置环境变量（生产环境必需）**
   ```bash
   cp .env.example .env
   # 编辑 .env 文件，修改密码和密钥
   ```

3. **启动服务**
   ```bash
   # 开发环境
   ./scripts/deploy.sh dev

   # 生产环境
   ./scripts/deploy.sh prod
   ```

4. **访问应用**
   - 开发环境: http://localhost:3000
   - 生产环境: http://localhost (或配置的域名)

---

## 手动部署

### 前提条件

- Docker >= 20.0
- Docker Compose >= 2.0
- Git

### 构建镜像

```bash
# 构建 API 服务
docker build -t testcase-api -f services/api/Dockerfile .

# 构建 Web 前端
docker build -t testcase-web -f apps/web/Dockerfile .
```

### 运行容器

```bash
# 运行数据库
docker run -d \
  --name testcase-postgres \
  -e POSTGRES_USER=testcase \
  -e POSTGRES_PASSWORD=testcase \
  -e POSTGRES_DB=testcase_manager \
  -v postgres_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:15-alpine

# 运行 Redis
docker run -d \
  --name testcase-redis \
  -v redis_data:/data \
  -p 6379:6379 \
  redis:7-alpine

# 运行 API
docker run -d \
  --name testcase-api \
  --link testcase-postgres \
  --link testcase-redis \
  -e DATABASE_URL=postgres://testcase:testcase@testcase-postgres:5432/testcase_manager \
  -e REDIS_URL=redis://testcase-redis:6379 \
  -p 3001:3001 \
  testcase-api

# 运行 Web
docker run -d \
  --name testcase-web \
  --link testcase-api \
  -p 3000:80 \
  testcase-web
```

---

## 生产环境配置

### 1. 修改环境变量

编辑 `.env` 文件：

```bash
# 强密码
DB_PASSWORD=your-super-secure-password
REDIS_PASSWORD=your-redis-password

# 随机 JWT 密钥
JWT_SECRET=$(openssl rand -base64 32)

# Web 端口
WEB_PORT=80
```

### 2. 使用 HTTPS

推荐使用反向代理（如 Nginx 或 Traefik）处理 HTTPS：

```yaml
# docker-compose.yml 添加
services:
  traefik:
    image: traefik:v2.10
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.tlschallenge=true"
      - "--certificatesresolvers.letsencrypt.acme.email=your@email.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
    ports:
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./letsencrypt:/letsencrypt
```

### 3. 数据备份

```bash
# 运行备份脚本
./scripts/backup.sh

# 或手动备份
docker exec testcase-postgres pg_dump -U testcase testcase_manager > backup.sql
```

---

## 更新升级

```bash
# 拉取最新代码
git pull origin main

# 重新构建并启动
./scripts/deploy.sh prod --pull
```

---

## 故障排除

### 查看日志

```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f api
```

### 重启服务

```bash
docker-compose restart api
```

### 重置数据

```bash
# 警告：这将删除所有数据！
docker-compose down -v
docker-compose up -d
```

---

## 系统要求

| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核心 | 4 核心 |
| 内存 | 4GB | 8GB |
| 磁盘 | 20GB | 50GB |
| 网络 | 可访问互联网 | 稳定网络连接 |

---

## 目录结构

```
testcase-manager/
├── services/api/          # API 服务
│   ├── Dockerfile
│   └── .dockerignore
├── apps/web/              # Web 前端
│   ├── Dockerfile
│   ├── .dockerignore
│   └── nginx.conf
├── scripts/
│   ├── deploy.sh          # 部署脚本
│   └── backup.sh          # 备份脚本
├── docker-compose.yml     # 开发环境配置
├── docker-compose.prod.yml # 生产环境配置
├── .env.example           # 环境变量示例
└── DEPLOY.md              # 本文件
```

---

## 安全建议

1. **修改默认密码** - 生产环境务必修改所有默认密码
2. **使用 HTTPS** - 生产环境必须启用 HTTPS
3. **定期备份** - 设置定时任务定期备份数据
4. **限制访问** - 使用防火墙限制不必要的端口访问
5. **更新镜像** - 定期更新基础镜像以获取安全补丁
