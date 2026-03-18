import { Router } from 'express';
import * as path from 'path';
import * as fs from 'fs-extra';
import { v4 as uuidv4 } from 'uuid';
import { GitService } from '@testcase-manager/git-core';
import type { Project } from '@testcase-manager/shared';

const router = Router();
const gitService = new GitService();

const DATA_DIR = path.resolve(process.cwd(), '..', '..', 'data', 'projects');

// Ensure data directory exists
fs.ensureDirSync(DATA_DIR);

/**
 * GET /api/projects - Get all projects
 */
router.get('/', async (req, res, next) => {
  try {
    const projects: Project[] = [];
    
    if (await fs.pathExists(DATA_DIR)) {
      const entries = await fs.readdir(DATA_DIR, { withFileTypes: true });
      
      for (const entry of entries) {
        if (entry.isDirectory()) {
          const projectFile = path.join(DATA_DIR, entry.name, 'project.json');
          if (await fs.pathExists(projectFile)) {
            const project = await fs.readJson(projectFile);
            projects.push(project);
          }
        }
      }
    }
    
    res.json({ projects });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /api/projects - Create a new project
 */
router.post('/', async (req, res, next) => {
  try {
    const { name, description, gitRepositoryUrl, clone } = req.body;
    
    if (!name) {
      res.status(400).json({ error: 'Project name is required' });
      return;
    }
    
    const projectId = uuidv4();
    const projectDir = path.join(DATA_DIR, projectId);
    const repoDir = path.join(projectDir, 'repo');
    
    await fs.ensureDir(projectDir);
    
    const project: Project = {
      id: projectId,
      name,
      description: description || '',
      testSuiteIds: [],
      testCaseFileIds: [],
      members: [],
      ownerId: 'current-user', // TODO: Get from auth
      status: 'active',
      gitRepositoryUrl,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    
    // Initialize or clone Git repository
    if (clone && gitRepositoryUrl) {
      await gitService.clone(gitRepositoryUrl, repoDir);
    } else {
      await gitService.init(repoDir);
      
      // Create initial directory structure
      const suitesDir = path.join(repoDir, 'suites');
      await fs.ensureDir(suitesDir);
      
      // Create initial commit
      await gitService.addAll(repoDir);
      await gitService.commit(repoDir, 'Initial commit', {
        name: 'TestCase Manager',
        email: 'system@testcase-manager.local',
        timestamp: new Date().toISOString(),
      });
    }
    
    await fs.writeJson(path.join(projectDir, 'project.json'), project, { spaces: 2 });
    
    res.status(201).json({ project });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/projects/:id - Get project details
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const projectFile = path.join(DATA_DIR, id, 'project.json');
    
    if (!(await fs.pathExists(projectFile))) {
      res.status(404).json({ error: 'Project not found' });
      return;
    }
    
    const project = await fs.readJson(projectFile);
    res.json({ project });
  } catch (error) {
    next(error);
  }
});

/**
 * DELETE /api/projects/:id - Delete a project
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const projectDir = path.join(DATA_DIR, id);
    
    if (!(await fs.pathExists(projectDir))) {
      res.status(404).json({ error: 'Project not found' });
      return;
    }
    
    await fs.remove(projectDir);
    res.status(204).send();
  } catch (error) {
    next(error);
  }
});

export { router as projectsRouter };
