cd /home/node/.openclaw/workspace/testcase-manager

# 清理所有 node_modules
echo "=== 清理所有 node_modules ==="
rm -rf node_modules package-lock.json
for dir in packages/shared packages/git-core packages/excel-core services/api apps/web apps/vscode-extension; do
  if [ -d "$dir" ]; then
    echo "清理 $dir/node_modules"
    rm -rf "$dir/node_modules" "$dir/package-lock.json"
  fi
done

# 重新安装根目录依赖
echo ""
echo "=== 安装根目录依赖 ==="
npm install

# 为每个子包安装依赖
echo ""
echo "=== 为子包安装依赖 ==="
for dir in packages/shared packages/git-core packages/excel-core services/api apps/web apps/vscode-extension; do
  if [ -d "$dir" ]; then
    echo ""
    echo "安装 $dir 依赖..."
    cd "$dir"
    npm install
    cd /home/node/.openclaw/workspace/testcase-manager
  fi
done

echo ""
echo "=== 验证 TypeScript 安装 ==="
npx tsc --version

echo ""
echo "=== 运行构建 ==="
npm run build