#!/bin/bash
set -e

cd /home/node/.openclaw/workspace/testcase-manager

echo "=== 为所有子包安装依赖 ==="

# 安装 packages/shared
echo ""
echo "安装 packages/shared..."
cd packages/shared
npm install
cd ../..

# 安装 packages/git-core
echo ""
echo "安装 packages/git-core..."
cd packages/git-core
npm install
cd ../..

# 安装 packages/excel-core
echo ""
echo "安装 packages/excel-core..."
cd packages/excel-core
npm install
cd ../..

# 安装 services/api
echo ""
echo "安装 services/api..."
cd services/api
npm install
cd ../..

# 安装 apps/web
echo ""
echo "安装 apps/web..."
cd apps/web
npm install
cd ../..

# 安装 apps/vscode-extension
echo ""
echo "安装 apps/vscode-extension..."
cd apps/vscode-extension
npm install
cd ../..

echo ""
echo "=== 运行构建 ==="
npm run build

echo ""
echo "=== 构建完成 ==="