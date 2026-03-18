#!/bin/bash
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  TestCase Manager 部署脚本${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装${NC}"
    echo "请访问 https://docs.docker.com/get-docker/ 安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装${NC}"
    echo "请访问 https://docs.docker.com/compose/install/ 安装 Docker Compose"
    exit 1
fi

# 确定 compose 命令
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# 解析参数
ENV=${1:-dev}
PULL=false

# 解析其他参数
for arg in "$@"; do
    case $arg in
        --pull)
            PULL=true
            ;;
        -h|--help)
            echo "用法: $0 [dev|prod] [--pull]"
            echo ""
            echo "选项:"
            echo "  dev       使用开发环境配置 (默认)"
            echo "  prod      使用生产环境配置"
            echo "  --pull    部署前拉取最新代码"
            echo "  -h, --help 显示此帮助信息"
            exit 0
            ;;
    esac
done

if [ "$ENV" = "prod" ]; then
    echo -e "${BLUE}使用生产环境配置${NC}"
    COMPOSE_FILE="docker-compose.prod.yml"
    
    # 检查环境变量文件
    if [ ! -f .env ]; then
        echo -e "${YELLOW}警告: .env 文件不存在${NC}"
        echo "请复制 .env.example 到 .env 并修改配置:"
        echo "  cp .env.example .env"
        echo ""
        read -p "是否继续使用默认配置? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
else
    echo -e "${BLUE}使用开发环境配置${NC}"
    COMPOSE_FILE="docker-compose.yml"
fi

# 拉取最新代码
if [ "$PULL" = true ]; then
    echo -e "${BLUE}拉取最新代码...${NC}"
    if ! git pull origin main; then
        echo -e "${YELLOW}警告: 拉取代码失败，继续使用本地代码${NC}"
    fi
fi

# 构建镜像
echo -e "${BLUE}构建 Docker 镜像...${NC}"
$COMPOSE_CMD -f $COMPOSE_FILE build

# 启动服务
echo -e "${BLUE}启动服务...${NC}"
$COMPOSE_CMD -f $COMPOSE_FILE up -d

# 等待服务启动
echo -e "${BLUE}等待服务启动...${NC}"
sleep 5

# 检查服务状态
echo ""
echo -e "${GREEN}服务状态:${NC}"
$COMPOSE_CMD -f $COMPOSE_FILE ps

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  部署完成!${NC}"
echo -e "${GREEN}================================${NC}"

if [ "$ENV" = "prod" ]; then
    echo -e "访问地址: http://localhost (或配置的域名)"
else
    echo -e "Web 前端: ${BLUE}http://localhost:3000${NC}"
    echo -e "API 服务: ${BLUE}http://localhost:3001${NC}"
fi

echo ""
echo "常用命令:"
echo "  查看日志: $COMPOSE_CMD -f $COMPOSE_FILE logs -f"
echo "  停止服务: $COMPOSE_CMD -f $COMPOSE_FILE down"
echo "  重启服务: $COMPOSE_CMD -f $COMPOSE_FILE restart"
