# Docker 部署实施计划 - 阶段 7

> **任务:** 为 TestCase Manager 添加 Docker 支持，实现一键部署。

## 目标

创建完整的 Docker 部署方案，包括：
- 各服务的 Dockerfile
- docker-compose.yml 编排
- 部署文档

---

## 任务清单

### 任务 7.1: 创建 API 服务 Dockerfile

**文件:**
- 创建: `services/api/Dockerfile`
- 创建: `services/api/.dockerignore`

#### 步骤 1: 创建 Dockerfile

```dockerfile
# services/api/Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY package*.json ./
COPY ../../packages ./packages

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建
RUN npm run build

# 生产镜像
FROM node:20-alpine

WORKDIR /app

# 安装 git（用于 git 操作）
RUN apk add --no-cache git

# 复制依赖
COPY package*.json ./
RUN npm ci --only=production

# 复制构建产物
COPY --from=builder /app/dist ./dist

# 暴露端口
EXPOSE 3001

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

# 启动命令
CMD ["node", "dist/server.js"]
```

#### 步骤 2: 创建 .dockerignore

```
node_modules
npm-debug.log
.env
.env.local
dist
coverage
.git
.gitignore
README.md
Dockerfile
.dockerignore
```

---

### 任务 7.2: 创建 Web 前端 Dockerfile

**文件:**
- 创建: `apps/web/Dockerfile`
- 创建: `apps/web/.dockerignore`
- 创建: `apps/web/nginx.conf`

#### 步骤 1: 创建 Dockerfile

```dockerfile
# apps/web/Dockerfile
# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY package*.json ./
COPY ../../packages ./packages

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建
RUN npm run build

# 生产阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
```

#### 步骤 2: 创建 .dockerignore

```
node_modules
npm-debug.log
.env
.env.local
dist
coverage
.git
.gitignore
README.md
Dockerfile
.dockerignore
```

#### 步骤 3: 创建 nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://api:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

### 任务 7.3: 创建根目录 docker-compose.yml

**文件:**
- 创建: `docker-compose.yml`
- 创建: `docker-compose.prod.yml`
- 创建: `.env.example`

#### 步骤 1: 创建 docker-compose.yml (开发环境)

```yaml
version: '3.8'

services:
  # PostgreSQL 数据库
  postgres:
    image: postgres:15-alpine
    container_name: testcase-postgres
    environment:
      POSTGRES_USER: testcase
      POSTGRES_PASSWORD: testcase
      POSTGRES_DB: testcase_manager
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U testcase"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: testcase-redis
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # API 服务
  api:
    build:
      context: .
      dockerfile: services/api/Dockerfile
    container_name: testcase-api
    environment:
      NODE_ENV: development
      PORT: 3001
      DATABASE_URL: postgres://testcase:testcase@postgres:5432/testcase_manager
      REDIS_URL: redis://redis:6379
      JWT_SECRET: your-jwt-secret-key
      GIT_STORAGE_PATH: /data/git-repos
    volumes:
      - git_repos:/data/git-repos
    ports:
      - "3001:3001"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:3001/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Web 前端
  web:
    build:
      context: .
      dockerfile: apps/web/Dockerfile
    container_name: testcase-web
    ports:
      - "3000:80"
    depends_on:
      - api
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost/"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  redis_data:
  git_repos:
```

#### 步骤 2: 创建 docker-compose.prod.yml (生产环境)

```yaml
version: '3.8'

services:
  # PostgreSQL 数据库
  postgres:
    image: postgres:15-alpine
    container_name: testcase-postgres
    environment:
      POSTGRES_USER: ${DB_USER:-testcase}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-changeme}
      POSTGRES_DB: ${DB_NAME:-testcase_manager}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - testcase-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-testcase}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: testcase-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:-changeme}
    volumes:
      - redis_data:/data
    networks:
      - testcase-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-changeme}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # API 服务
  api:
    build:
      context: .
      dockerfile: services/api/Dockerfile
    container_name: testcase-api
    environment:
      NODE_ENV: production
      PORT: 3001
      DATABASE_URL: postgres://${DB_USER:-testcase}:${DB_PASSWORD:-changeme}@postgres:5432/${DB_NAME:-testcase_manager}
      REDIS_URL: redis://:${REDIS_PASSWORD:-changeme}@redis:6379
      JWT_SECRET: ${JWT_SECRET}
      GIT_STORAGE_PATH: /data/git-repos
    volumes:
      - git_repos:/data/git-repos
    networks:
      - testcase-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:3001/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Web 前端
  web:
    build:
      context: .
      dockerfile: apps/web/Dockerfile
    container_name: testcase-web
    ports:
      - "${WEB_PORT:-80}:80"
    networks:
      - testcase-network
    depends_on:
      - api
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost/"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  git_repos:

networks:
  testcase-network:
    driver: bridge
```

#### 步骤 3: 创建 .env.example

```bash
# 数据库配置
DB_USER=testcase
DB_PASSWORD=your-secure-password
DB_NAME=testcase_manager

# Redis 配置
REDIS_PASSWORD=your-redis-password

# JWT 密钥（生产环境必须修改）
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# Web 端口（生产环境）
WEB_PORT=80
```

---

### 任务 7.4: 创建部署脚本

**文件:**
- 创建: `scripts/deploy.sh`
- 创建: `scripts/backup.sh`

#### 步骤 1: 创建 deploy.sh

```bash
#!/bin/bash
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}TestCase Manager 部署脚本${NC}"
echo "================================"

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装${NC}"
    exit 1
fi

# 检查环境变量文件
if [ ! -f .env ]; then
    echo -e "${YELLOW}警告: .env 文件不存在，使用默认配置${NC}"
    echo "请复制 .env.example 到 .env 并修改配置"
fi

# 解析参数
ENV=${1:-dev}

if [ "$ENV" = "prod" ]; then
    echo -e "${GREEN}使用生产环境配置${NC}"
    COMPOSE_FILE="docker-compose.prod.yml"
else
    echo -e "${GREEN}使用开发环境配置${NC}"
    COMPOSE_FILE="docker-compose.yml"
fi

# 拉取最新代码（可选）
if [ "$2" = "--pull" ]; then
    echo -e "${GREEN}拉取最新代码...${NC}"
    git pull origin main
fi

# 构建镜像
echo -e "${GREEN}构建 Docker 镜像...${NC}"
docker-compose -f $COMPOSE_FILE build

# 启动服务
echo -e "${GREEN}启动服务...${NC}"
docker-compose -f $COMPOSE_FILE up -d

# 等待服务启动
echo -e "${GREEN}等待服务启动...${NC}"
sleep 10

# 检查服务状态
echo -e "${GREEN}检查服务状态...${NC}"
docker-compose -f $COMPOSE_FILE ps

echo ""
echo -e "${GREEN}部署完成!${NC}"
if [ "$ENV" = "prod" ]; then
    echo "访问: http://localhost (或配置的域名)"
else
    echo "Web: http://localhost:3000"
    echo "API: http://localhost:3001"
fi
```

#### 步骤 2: 创建 backup.sh

```bash
#!/bin/bash
set -e

# 颜色定义
GREEN='\033[0;32m'
NC='\033[0m'

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo -e "${GREEN}TestCase Manager 备份脚本${NC}"
echo "================================"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
echo "备份数据库..."
docker exec testcase-postgres pg_dump -U testcase testcase_manager > $BACKUP_DIR/db_$TIMESTAMP.sql

# 备份 Git 仓库
echo "备份 Git 仓库..."
tar -czf $BACKUP_DIR/git_repos_$TIMESTAMP.tar.gz -C /var/lib/docker/volumes/testcase-manager_git_repos/_data .

echo -e "${GREEN}备份完成: $BACKUP_DIR${NC}"
ls -lh $BACKUP_DIR
```

---

### 任务 7.5: 更新文档

**文件:**
- 修改: `README.md` 添加部署说明
- 创建: `DEPLOY.md` 详细部署文档

#### 步骤 1: 创建 DEPLOY.md

```markdown
# TestCase Manager 部署指南

## 快速开始

### 使用 Docker Compose（推荐）

1. **克隆仓库**
   ```bash
   git clone https://github.com/your-org/testcase-manager.git
   cd testcase-manager
   ```

2. **配置环境变量**
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

## 更新升级

```bash
# 拉取最新代码
git pull origin main

# 重新构建并启动
./scripts/deploy.sh prod --pull
```

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

## 系统要求

- **CPU**: 2 核心+
- **内存**: 4GB+
- **磁盘**: 20GB+
- **网络**: 可访问互联网（用于拉取镜像）
```

---

### 任务 7.6: 提交代码

```bash
git add .
git commit -m "feat(docker): add Docker deployment support with compose and scripts"
```

---

**计划完成！** 准备开始实施。
