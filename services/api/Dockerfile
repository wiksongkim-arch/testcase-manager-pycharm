# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 安装 Python 和构建工具（某些原生模块需要）
RUN apk add --no-cache python3 make g++

# 复制根 package.json
COPY package*.json ./

# 复制 workspace 配置
COPY packages ./packages

# 安装所有依赖（包括 devDependencies）
RUN npm ci

# 构建 packages
RUN npm run build

# 复制 API 服务的 package.json
COPY services/api/package*.json ./services/api/

# 安装 API 服务依赖
WORKDIR /app/services/api
RUN npm ci

# 复制 API 源代码
COPY services/api/src ./src
COPY services/api/tsconfig.json ./

# 构建 API
RUN npm run build

# 生产镜像
FROM node:20-alpine

WORKDIR /app

# 安装 git（用于 git 操作）和 wget（用于健康检查）
RUN apk add --no-cache git wget

# 复制根 package.json
COPY package*.json ./

# 复制已构建的 packages
COPY --from=builder /app/packages ./packages

# 复制 API 服务文件
COPY services/api/package*.json ./

# 只安装生产依赖
RUN npm ci --only=production

# 复制构建产物
COPY --from=builder /app/services/api/dist ./dist

# 创建数据目录
RUN mkdir -p /data/git-repos

# 暴露端口
EXPOSE 3001

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

# 启动命令
CMD ["node", "dist/index.js"]
