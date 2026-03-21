# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 安装构建工具
RUN apk add --no-cache python3 make g++ git

# 安装 pnpm
RUN npm install -g pnpm

# 复制项目文件
COPY package*.json pnpm-lock.yaml .npmrc pnpm-workspace.yaml ./
COPY packages ./packages
COPY scripts ./scripts

# 安装依赖并构建
RUN pnpm install --frozen-lockfile
RUN pnpm run build

# 复制并构建 API
COPY services/api ./services/api
WORKDIR /app/services/api
RUN pnpm install --frozen-lockfile
RUN pnpm run build

# 生产镜像
FROM node:20-alpine

WORKDIR /app

# 安装必要工具
RUN apk add --no-cache git wget

# 从 builder 复制 API 服务（包含所有依赖）
COPY --from=builder /app/services/api /app

# 复制 packages（workspace 依赖需要）
COPY --from=builder /app/packages /app/packages

# 创建数据目录
RUN mkdir -p /data/git-repos

EXPOSE 3001

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

CMD ["node", "dist/index.js"]
