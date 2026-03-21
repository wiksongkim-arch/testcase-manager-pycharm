FROM node:20-alpine

WORKDIR /app

# 安装构建工具
RUN apk add --no-cache python3 make g++ git wget

# 安装 pnpm
RUN npm install -g pnpm

# 复制项目文件
COPY package*.json pnpm-lock.yaml .npmrc pnpm-workspace.yaml ./
COPY packages ./packages
COPY scripts ./scripts
COPY services/api ./services/api

# 安装所有依赖
RUN pnpm install --frozen-lockfile

# 构建 packages
RUN pnpm run build

# 构建 API
WORKDIR /app/services/api
RUN pnpm install --frozen-lockfile
RUN pnpm run build

# 创建数据目录
RUN mkdir -p /data/git-repos

EXPOSE 3001

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1

CMD ["node", "dist/index.js"]
