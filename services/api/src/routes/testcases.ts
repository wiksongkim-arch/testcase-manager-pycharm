import { Router } from 'express';
import * as path from 'path';
import * as fs from 'fs-extra';
import { v4 as uuidv4 } from 'uuid';
import type { TestCaseFile, TestSuite } from '@testcase-manager/shared';

const router = Router();

const DATA_DIR = path.resolve(process.cwd(), '..', '..', 'data', 'projects');

// Helper to get project repo path
function getProjectRepoPath(projectId: string): string {
  return path.join(DATA_DIR, projectId, 'repo');
}

// Helper to get suites directory
function getSuitesDir(projectId: string): string {
  return path.join(getProjectRepoPath(projectId), 'suites');
}

// Helper to get suite file path
function getSuitePath(projectId: string, suiteId: string): string {
  return path.join(getSuitesDir(projectId), suiteId, 'cases.json');
}

/**
 * GET /api/testcases/:projectId/suites - Get all test suites for a project
 */
router.get('/:projectId/suites', async (req, res, next) => {
  try {
    const { projectId } = req.params;
    const suitesDir = getSuitesDir(projectId);
    
    if (!(await fs.pathExists(suitesDir))) {
      res.json({ suites: [] });
      return;
    }
    
    const suites: TestSuite[] = [];
    const entries = await fs.readdir(suitesDir, { withFileTypes: true });
    
    for (const entry of entries) {
      if (entry.isDirectory()) {
        const suiteFile = path.join(suitesDir, entry.name, 'cases.json');
        if (await fs.pathExists(suiteFile)) {
          const testCaseFile = await fs.readJson(suiteFile);
          // Create a suite from the test case file
          const suite: TestSuite = {
            id: entry.name,
            name: testCaseFile.name || entry.name,
            description: testCaseFile.description || '',
            testCaseFileIds: [entry.name],
            tags: [],
            createdAt: testCaseFile.createdAt || new Date().toISOString(),
            updatedAt: testCaseFile.updatedAt || new Date().toISOString(),
            createdBy: 'system',
          };
          suites.push(suite);
        }
      }
    }
    
    res.json({ suites });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/testcases/:projectId/suites/:suiteId - Get test case file content
 */
router.get('/:projectId/suites/:suiteId', async (req, res, next) => {
  try {
    const { projectId, suiteId } = req.params;
    const suitePath = getSuitePath(projectId, suiteId);
    
    if (!(await fs.pathExists(suitePath))) {
      res.status(404).json({ error: 'Test suite not found' });
      return;
    }
    
    const testCaseFile: TestCaseFile = await fs.readJson(suitePath);
    res.json({ testCaseFile });
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /api/testcases/:projectId/suites/:suiteId - Update test case file
 */
router.put('/:projectId/suites/:suiteId', async (req, res, next) => {
  try {
    const { projectId, suiteId } = req.params;
    const updates: Partial<TestCaseFile> = req.body;
    const suitePath = getSuitePath(projectId, suiteId);
    
    if (!(await fs.pathExists(suitePath))) {
      res.status(404).json({ error: 'Test suite not found' });
      return;
    }
    
    const existingFile: TestCaseFile = await fs.readJson(suitePath);
    const updatedFile: TestCaseFile = {
      ...existingFile,
      ...updates,
      updatedAt: new Date().toISOString(),
    };
    
    await fs.writeJson(suitePath, updatedFile, { spaces: 2 });
    res.json({ testCaseFile: updatedFile });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /api/testcases/:projectId/suites - Create a new test suite
 */
router.post('/:projectId/suites', async (req, res, next) => {
  try {
    const { projectId } = req.params;
    const { name, description } = req.body;
    
    if (!name) {
      res.status(400).json({ error: 'Suite name is required' });
      return;
    }
    
    const suiteId = uuidv4();
    const suiteDir = path.join(getSuitesDir(projectId), suiteId);
    const suitePath = path.join(suiteDir, 'cases.json');
    
    await fs.ensureDir(suiteDir);
    
    const now = new Date().toISOString();
    const testCaseFile: TestCaseFile = {
      version: '1.0',
      name,
      description: description || '',
      columns: [
        {
          id: 'id',
          name: 'ID',
          type: 'text',
          required: true,
          width: 100,
          order: 0,
          visible: true,
        },
        {
          id: 'title',
          name: 'Title',
          type: 'text',
          required: true,
          width: 300,
          order: 1,
          visible: true,
        },
        {
          id: 'priority',
          name: 'Priority',
          type: 'select',
          options: ['low', 'medium', 'high', 'critical'],
          width: 120,
          order: 2,
          visible: true,
        },
        {
          id: 'status',
          name: 'Status',
          type: 'select',
          options: ['draft', 'ready', 'deprecated', 'archived'],
          width: 120,
          order: 3,
          visible: true,
        },
        {
          id: 'tags',
          name: 'Tags',
          type: 'tags',
          width: 200,
          order: 4,
          visible: true,
        },
      ],
      rows: [],
      settings: {
        autoSaveInterval: 30,
        defaultColumnWidth: 150,
        showRowNumbers: true,
        showGridLines: true,
        timezone: 'UTC',
        dateFormat: 'YYYY-MM-DD',
        versionControl: true,
        conflictResolution: 'manual',
      },
      createdAt: now,
      updatedAt: now,
    };
    
    await fs.writeJson(suitePath, testCaseFile, { spaces: 2 });
    
    const suite: TestSuite = {
      id: suiteId,
      name,
      description: description || '',
      testCaseFileIds: [suiteId],
      tags: [],
      createdAt: now,
      updatedAt: now,
      createdBy: 'current-user',
    };
    
    res.status(201).json({ suite, testCaseFile });
  } catch (error) {
    next(error);
  }
});

export { router as testCasesRouter };
