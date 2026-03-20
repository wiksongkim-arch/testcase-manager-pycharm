#!/usr/bin/env node
/**
 * Build script for TestCase Manager monorepo
 * Builds packages in correct dependency order
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Packages in dependency order (shared must be first)
const packages = [
  { name: 'shared', path: 'packages/shared' },
  { name: 'git-core', path: 'packages/git-core' },
  { name: 'excel-core', path: 'packages/excel-core' },
  { name: 'api', path: 'services/api' },
  { name: 'web', path: 'apps/web' },
  { name: 'vscode-extension', path: 'apps/vscode-extension' },
];

console.log('🔨 Building TestCase Manager monorepo...\n');

// Get the tsc and vite paths from root node_modules
const rootDir = path.join(__dirname, '..');
const tscPath = path.join(rootDir, 'node_modules', '.bin', 'tsc');
const vitePath = path.join(rootDir, 'node_modules', '.bin', 'vite');

for (const pkg of packages) {
  const pkgPath = path.join(rootDir, pkg.path);
  
  // Check if package exists
  if (!fs.existsSync(pkgPath)) {
    console.log(`⚠️  Skipping ${pkg.name} (directory not found)`);
    continue;
  }
  
  console.log(`📦 Building ${pkg.name}...`);
  
  try {
    // Use direct commands to avoid npm workspaces circular dependency
    let buildCmd;
    if (pkg.name === 'web') {
      // web needs both tsc and vite build - use npx to resolve local vite
      buildCmd = `${tscPath} && npx vite build`;
    } else {
      // All other packages just need tsc
      buildCmd = `${tscPath}`;
    }
    
    execSync(buildCmd, {
      cwd: pkgPath,
      stdio: 'inherit',
      env: { ...process.env, PATH: process.env.PATH }
    });
    console.log(`✅ ${pkg.name} built successfully\n`);
  } catch (error) {
    console.error(`❌ Failed to build ${pkg.name}`);
    console.error(error.message);
    process.exit(1);
  }
}

console.log('✅ All packages built successfully!');
