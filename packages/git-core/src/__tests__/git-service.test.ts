/**
 * Unit tests for GitService
 * Note: These tests use temporary directories and mock filesystem operations
 */

import * as fs from 'fs-extra';
import * as path from 'path';
import * as os from 'os';
import { GitService } from '../git-service';
import type { GitAuthor, HttpsCredentials } from '@testcase-manager/shared';

function createAuthor(name: string, email: string): GitAuthor {
  return { name, email, timestamp: new Date().toISOString() };
}

describe('GitService', () => {
  let gitService: GitService;
  let tempDir: string;

  beforeEach(async () => {
    gitService = new GitService();
    tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'git-service-test-'));
  });

  afterEach(async () => {
    await fs.remove(tempDir);
  });

  describe('init', () => {
    it('should initialize a new git repository', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      
      await gitService.init(repoPath);
      
      const gitDir = path.join(repoPath, '.git');
      expect(await fs.pathExists(gitDir)).toBe(true);
    });

    it('should create the directory if it does not exist', async () => {
      const repoPath = path.join(tempDir, 'nested', 'test-repo');
      
      await gitService.init(repoPath);
      
      expect(await fs.pathExists(repoPath)).toBe(true);
    });
  });

  describe('isRepo', () => {
    it('should return true for a git repository', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      const result = await gitService.isRepo(repoPath);
      
      expect(result).toBe(true);
    });

    it('should return false for a non-git directory', async () => {
      const nonRepoPath = path.join(tempDir, 'not-a-repo');
      await fs.ensureDir(nonRepoPath);
      
      const result = await gitService.isRepo(nonRepoPath);
      
      expect(result).toBe(false);
    });
  });

  describe('add and commit', () => {
    it('should add and commit a file', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      // Create a test file
      const testFile = path.join(repoPath, 'test.txt');
      await fs.writeFile(testFile, 'Hello World');
      
      // Add and commit
      await gitService.add(repoPath, 'test.txt');
      const author = createAuthor('Test User', 'test@example.com');
      const commitOid = await gitService.commit(repoPath, 'Initial commit', author);
      
      expect(commitOid).toBeDefined();
      expect(typeof commitOid).toBe('string');
    });

    it('should add all files', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      // Create test files
      await fs.writeFile(path.join(repoPath, 'file1.txt'), 'Content 1');
      await fs.writeFile(path.join(repoPath, 'file2.txt'), 'Content 2');
      
      // Add all and commit
      await gitService.addAll(repoPath);
      const author = createAuthor('Test User', 'test@example.com');
      const commitOid = await gitService.commit(repoPath, 'Add all files', author);
      
      expect(commitOid).toBeDefined();
    });
  });

  describe('getStatus', () => {
    it('should return clean status for empty repo', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      const status = await gitService.getStatus(repoPath);
      
      expect(status.isClean).toBe(true);
      expect(status.currentBranch).toBe('main');
    });

    it('should detect untracked files', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      await fs.writeFile(path.join(repoPath, 'untracked.txt'), 'Content');
      
      const status = await gitService.getStatus(repoPath);
      
      expect(status.isClean).toBe(false);
      expect(status.untrackedCount).toBeGreaterThan(0);
    });

    it('should detect staged files', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      await fs.writeFile(path.join(repoPath, 'staged.txt'), 'Content');
      await gitService.add(repoPath, 'staged.txt');
      
      const status = await gitService.getStatus(repoPath);
      
      expect(status.stagedCount).toBeGreaterThan(0);
    });
  });

  describe('getCurrentBranch', () => {
    it('should return main for newly initialized repo', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      const branch = await gitService.getCurrentBranch(repoPath);
      
      expect(branch).toBe('main');
    });
  });

  describe('getBranches', () => {
    it('should list branches', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      // Create initial commit
      await fs.writeFile(path.join(repoPath, 'file.txt'), 'content');
      await gitService.add(repoPath, 'file.txt');
      await gitService.commit(repoPath, 'Initial', createAuthor('Test', 'test@test.com'));
      
      // Create a new branch
      await gitService.createBranch(repoPath, 'feature-branch');
      
      const branches = await gitService.getBranches(repoPath);
      
      expect(branches.length).toBeGreaterThanOrEqual(1);
      expect(branches.some(b => b.name === 'main')).toBe(true);
      expect(branches.some(b => b.name === 'feature-branch')).toBe(true);
    });
  });

  describe('createBranch and checkout', () => {
    it('should create and checkout a new branch', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      // Create initial commit
      await fs.writeFile(path.join(repoPath, 'file.txt'), 'content');
      await gitService.add(repoPath, 'file.txt');
      await gitService.commit(repoPath, 'Initial', createAuthor('Test', 'test@test.com'));
      
      // Create and checkout new branch
      await gitService.createBranch(repoPath, 'new-branch');
      await gitService.checkout(repoPath, 'new-branch');
      
      const currentBranch = await gitService.getCurrentBranch(repoPath);
      expect(currentBranch).toBe('new-branch');
    });
  });

  describe('getCommitHistory', () => {
    it('should return commit history', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      // Create initial commit
      await fs.writeFile(path.join(repoPath, 'file.txt'), 'content');
      await gitService.add(repoPath, 'file.txt');
      await gitService.commit(repoPath, 'First commit', createAuthor('Test', 'test@test.com'));
      
      const history = await gitService.getCommitHistory(repoPath);
      
      expect(history.length).toBeGreaterThan(0);
      expect(history[0].message).toContain('First commit');
      expect(history[0].author.name).toBe('Test');
    });
  });

  describe('getConflicts', () => {
    it('should return empty array when no conflicts', async () => {
      const repoPath = path.join(tempDir, 'test-repo');
      await gitService.init(repoPath);
      
      const conflicts = await gitService.getConflicts(repoPath);
      
      expect(conflicts).toEqual([]);
    });
  });
});
