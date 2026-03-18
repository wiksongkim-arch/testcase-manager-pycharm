# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 复制根 package.json
COPY package*.json ./

# 复制 workspace 配置
COPY packages ./packages

# 安装所有依赖
RUN npm ci

# 构建 packages
RUN npm run build

# 复制 Web 服务的 package.json
COPY apps/web/package*.json ./apps/web/

# 安装 Web 服务依赖
WORKDIR /app/apps/web
RUN npm ci

# 复制 Web 源代码
COPY apps/web/src ./src
COPY apps/web/index.html ./
COPY apps/web/tsconfig.json ./
COPY apps/web/tsconfig.node.json ./
COPY apps/web/vite.config.ts ./
COPY apps/web/eslint.config.js ./
COPY apps/web/public ./public

# 构建 Web
RUN npm run build

# 生产阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/apps/web/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
