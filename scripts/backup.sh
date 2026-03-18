#!/bin/bash
set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 确定 compose 命令
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  TestCase Manager 备份脚本${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# 创建备份目录
mkdir -p $BACKUP_DIR

echo -e "${BLUE}备份目录: $BACKUP_DIR${NC}"
echo -e "${BLUE}备份时间: $(date)${NC}"
echo ""

# 备份数据库
echo -e "${BLUE}正在备份数据库...${NC}"
if docker ps | grep -q testcase-postgres; then
    docker exec testcase-postgres pg_dump -U testcase testcase_manager > $BACKUP_DIR/db_$TIMESTAMP.sql
    echo -e "${GREEN}✓ 数据库备份完成${NC}"
else
    echo -e "${YELLOW}⚠ PostgreSQL 容器未运行，跳过数据库备份${NC}"
fi

# 备份 Git 仓库
echo -e "${BLUE}正在备份 Git 仓库...${NC}"
if docker volume ls | grep -q testcase-manager_git_repos; then
    docker run --rm -v testcase-manager_git_repos:/data -v $(pwd)/$BACKUP_DIR:/backup alpine \
        tar -czf /backup/git_repos_$TIMESTAMP.tar.gz -C /data .
    echo -e "${GREEN}✓ Git 仓库备份完成${NC}"
else
    echo -e "${YELLOW}⚠ Git 仓库卷不存在，跳过备份${NC}"
fi

# 备份环境配置
echo -e "${BLUE}正在备份环境配置...${NC}"
if [ -f .env ]; then
    cp .env $BACKUP_DIR/env_$TIMESTAMP.backup
    echo -e "${GREEN}✓ 环境配置备份完成${NC}"
else
    echo -e "${YELLOW}⚠ .env 文件不存在，跳过环境配置备份${NC}"
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  备份完成!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "备份文件:"
ls -lh $BACKUP_DIR/*$TIMESTAMP*

echo ""
echo "备份位置: $BACKUP_DIR"
