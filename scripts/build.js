const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Find typescript compiler in workspaces
function findTsc() {
  // Possible locations for tsc
  const possiblePaths = [
    path.join(__dirname, '..', 'node_modules', 'typescript', 'bin', 'tsc'),
    path.join(__dirname, '..', 'packages', 'shared', 'node_modules', 'typescript', 'bin', 'tsc'),
    path.join(__dirname, '..', 'packages', 'git-core', 'node_modules', 'typescript', 'bin', 'tsc'),
    path.join(__dirname, '..', 'packages', 'excel-core', 'node_modules', 'typescript', 'bin', 'tsc'),
    path.join(__dirname, '..', 'apps', 'web', 'node_modules', 'typescript', 'bin', 'tsc'),
    path.join(__dirname, '..', 'services', 'api', 'node_modules', 'typescript', 'bin', 'tsc'),
  ];
  
  for (const tscPath of possiblePaths) {
    if (fs.existsSync(tscPath)) {
      console.log(`Found TypeScript at: ${tscPath}`);
      return tscPath;
    }
  }
  
  throw new Error('TypeScript not found. Please run: npm install typescript --save-dev');
}

const tscPath = findTsc();
console.log(`Using TypeScript compiler: ${tscPath}`);

const packages = ['packages/shared', 'packages/git-core'];

for (const pkg of packages) {
  console.log(`\nBuilding ${pkg}...`);
  const pkgPath = path.join(__dirname, '..', pkg);
  try {
    execSync(`node "${tscPath}"`, {
      cwd: pkgPath,
      stdio: 'inherit'
    });
    console.log(`✅ ${pkg} built successfully`);
  } catch (error) {
    console.error(`❌ Failed to build ${pkg}`);
    process.exit(1);
  }
}

console.log('\n✅ All packages built successfully!');
