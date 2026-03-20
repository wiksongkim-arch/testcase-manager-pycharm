import { Router } from 'express';
import * as path from 'path';
import * as fs from 'fs-extra';
import { GitService } from '@testcase-manager/git-core';
import type { GitAuthor } from '@testcase-manager/shared';

const router = Router();
const gitService = new GitService();

const DATA_DIR = path.resolve(process.cwd(), '..', '..', 'data', 'projects');

// Helper to get project repo path
function getProjectRepoPath(projectId: string): string {
  return path.join(DATA_DIR, projectId, 'repo');
}

/**
 * POST /commit - Commit changes
 */
router.post('/commit', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { message, author } = req.body;
    
    if (!message) {
      res.status(400).json({ error: 'Commit message is required' });
      return;
    }
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    // Stage all changes
    await gitService.addAll(repoPath);
    
    // Create commit
    const commitAuthor: GitAuthor = author || {
      name: 'TestCase Manager',
      email: 'system@testcase-manager.local',
      timestamp: new Date().toISOString(),
    };
    
    const sha = await gitService.commit(repoPath, message, commitAuthor);
    
    res.json({ 
      success: true, 
      sha,
      message,
      author: commitAuthor,
    });
  } catch (error: any) {
    // Handle no changes to commit
    if (error.message?.includes('nothing to commit')) {
      res.json({ success: true, message: 'No changes to commit' });
      return;
    }
    next(error);
  }
});

/**
 * POST /push - Push to remote
 */
router.post('/:projectId/push', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { remote, branch, credentials } = req.body;
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const currentBranch = await gitService.getCurrentBranch(repoPath);
    const targetBranch = branch || currentBranch;
    const targetRemote = remote || 'origin';
    
    const result = await gitService.push(
      repoPath,
      targetRemote,
      targetBranch,
      credentials
    );
    
    res.json(result);
  } catch (error) {
    next(error);
  }
});

/**
 * POST /pull - Pull updates
 */
router.post('/:projectId/pull', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { remote, branch, credentials } = req.body;
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const currentBranch = await gitService.getCurrentBranch(repoPath);
    const targetBranch = branch || currentBranch;
    const targetRemote = remote || 'origin';
    
    const result = await gitService.pull(
      repoPath,
      targetRemote,
      targetBranch,
      credentials
    );
    
    res.json(result);
  } catch (error) {
    next(error);
  }
});

/**
 * GET /status - Get repository status
 */
router.get('/:projectId/status', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const status = await gitService.getStatus(repoPath);
    res.json({ status });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /branches - Get branch list
 */
router.get('/:projectId/branches', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const branches = await gitService.getBranches(repoPath);
    res.json({ branches });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /branches - Create a new branch
 */
router.post('/:projectId/branches', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { name } = req.body;
    
    if (!name) {
      res.status(400).json({ error: 'Branch name is required' });
      return;
    }
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    await gitService.createBranch(repoPath, name);
    
    const branches = await gitService.getBranches(repoPath);
    const newBranch = branches.find(b => b.name === name);
    
    res.status(201).json({ branch: newBranch });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /checkout - Switch branch
 */
router.post('/:projectId/checkout', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { branch } = req.body;
    
    if (!branch) {
      res.status(400).json({ error: 'Branch name is required' });
      return;
    }
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    await gitService.checkout(repoPath, branch);
    
    res.json({ success: true, branch });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /history - Get commit history
 */
router.get('/:projectId/history', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { depth } = req.query;
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const commits = await gitService.getCommitHistory(
      repoPath,
      'HEAD',
      depth ? parseInt(depth as string, 10) : 20
    );
    
    res.json({ commits });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /conflicts - Get conflict files
 */
router.get('/:projectId/conflicts', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    const conflicts = await gitService.getConflicts(repoPath);
    res.json({ conflicts });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /resolve - Resolve conflicts
 */
router.post('/:projectId/resolve', async (req, res, next) => {
  try {
    const { projectId } = req.params as { projectId: string };
    const { filePath, content } = req.body;
    
    if (!filePath) {
      res.status(400).json({ error: 'File path is required' });
      return;
    }
    
    const repoPath = getProjectRepoPath(projectId);
    
    if (!(await fs.pathExists(repoPath))) {
      res.status(404).json({ error: 'Project repository not found' });
      return;
    }
    
    // Write resolved content
    const fullPath = path.join(repoPath, filePath);
    await fs.writeFile(fullPath, content, 'utf-8');
    
    // Stage the resolved file
    await gitService.add(repoPath, filePath);
    
    res.json({ success: true, filePath });
  } catch (error) {
    next(error);
  }
});

export { router as gitRouter };
