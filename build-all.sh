#!/bin/bash
set -e

cd /home/node/.openclaw/workspace/testcase-manager

TSC="/home/node/.openclaw/workspace/testcase-manager/node_modules/.bin/tsc"

echo "=== 构建 packages/shared ==="
cd packages/shared
$TSC
cd ../..

echo ""
echo "=== 构建 packages/git-core ==="
cd packages/git-core
npm install
$TSC
cd ../..

echo ""
echo "=== 构建 packages/excel-core ==="
cd packages/excel-core
npm install
$TSC
cd ../..

echo ""
echo "=== 构建 services/api ==="
cd services/api
npm install
$TSC
cd ../..

echo ""
echo "=== 构建 apps/web ==="
cd apps/web
npm install
npm run build
cd ../..

echo ""
echo "=== 构建 apps/vscode-extension ==="
cd apps/vscode-extension
npm install
npm run build
cd ../..

echo ""
echo "=== 所有包构建完成 ==="