import { Router } from 'express';
import multer from 'multer';
import * as path from 'path';
import * as fs from 'fs-extra';
import { v4 as uuidv4 } from 'uuid';
import { excelImporter, excelExporter } from '@testcase-manager/excel-core';
import type { TestCaseFile } from '@testcase-manager/shared';

const router = Router();
const upload = multer({ storage: multer.memoryStorage() });

const DATA_DIR = path.resolve(process.cwd(), '..', '..', 'data', 'projects');

// Helper to get project repo path
function getProjectRepoPath(projectId: string): string {
  return path.join(DATA_DIR, projectId, 'repo');
}

// Helper to get suites directory
function getSuitesDir(projectId: string): string {
  return path.join(getProjectRepoPath(projectId), 'suites');
}

/**
 * POST /api/projects/:projectId/import - 导入 Excel 文件
 */
router.post(
  '/:projectId/import',
  upload.single('file'),
  async (req, res, next) => {
    try {
      const { projectId } = req.params;
      const file = req.file;
      const { suiteId, columnMapping, startRow } = req.body;
      
      if (!file) {
        res.status(400).json({ error: '请上传文件' });
        return;
      }
      
      // 检查文件类型
      const ext = path.extname(file.originalname).toLowerCase();
      if (!['.xlsx', '.xls', '.csv'].includes(ext)) {
        res.status(400).json({ error: '只支持 .xlsx, .xls 或 .csv 文件' });
        return;
      }
      
      // 检查项目是否存在
      const projectDir = path.join(DATA_DIR, projectId);
      if (!(await fs.pathExists(projectDir))) {
        res.status(404).json({ error: '项目不存在' });
        return;
      }
      
      // 解析列映射（如果提供）
      let parsedColumnMapping: Record<string, string> | undefined;
      if (columnMapping) {
        try {
          parsedColumnMapping = typeof columnMapping === 'string' 
            ? JSON.parse(columnMapping) 
            : columnMapping;
        } catch {
          res.status(400).json({ error: '列映射格式无效' });
          return;
        }
      }
      
      // 导入数据
      const result = await excelImporter.importFromBuffer(file.buffer, {
        columnMapping: parsedColumnMapping,
        startRow: startRow ? parseInt(startRow, 10) : undefined,
      });
      
      // 保存到项目
      if (result.importedCount > 0) {
        const suitesDir = getSuitesDir(projectId);
        await fs.ensureDir(suitesDir);
        
        // 使用提供的 suiteId 或创建新的
        const targetSuiteId = suiteId || uuidv4();
        const suiteDir = path.join(suitesDir, targetSuiteId);
        await fs.ensureDir(suiteDir);
        
        const suitePath = path.join(suiteDir, 'cases.json');
        
        // 如果 suite 已存在，合并数据
        if (await fs.pathExists(suitePath)) {
          const existingFile: TestCaseFile = await fs.readJson(suitePath);
          result.testCaseFile.rows = [...existingFile.rows, ...result.testCaseFile.rows];
          result.testCaseFile.name = existingFile.name;
          result.testCaseFile.description = existingFile.description;
        } else {
          // 使用文件名作为 suite 名称
          const baseName = path.basename(file.originalname, ext);
          result.testCaseFile.name = baseName;
        }
        
        await fs.writeJson(suitePath, result.testCaseFile, { spaces: 2 });
      }
      
      res.json({
        success: true,
        imported: result.importedCount,
        total: result.totalRows,
        skipped: result.skippedRows.length,
        errors: result.errors,
        suiteId: suiteId,
      });
    } catch (error: any) {
      next(error);
    }
  }
);

/**
 * GET /api/projects/:projectId/export - 导出 Excel 文件
 */
router.get('/:projectId/export', async (req, res, next) => {
  try {
    const { projectId } = req.params;
    const { 
      format = 'xlsx', 
      suiteId,
      sheetName,
    } = req.query as { 
      format?: 'xlsx' | 'csv'; 
      suiteId?: string;
      sheetName?: string;
    };
    
    // 检查项目是否存在
    const projectDir = path.join(DATA_DIR, projectId);
    if (!(await fs.pathExists(projectDir))) {
      res.status(404).json({ error: '项目不存在' });
      return;
    }
    
    // 获取测试用例
    let testCaseFile: TestCaseFile | null = null;
    
    if (suiteId) {
      // 导出特定 suite
      const suitePath = path.join(getSuitesDir(projectId), suiteId, 'cases.json');
      if (!(await fs.pathExists(suitePath))) {
        res.status(404).json({ error: '测试套件不存在' });
        return;
      }
      testCaseFile = await fs.readJson(suitePath);
    } else {
      // 导出所有 suites
      const suitesDir = getSuitesDir(projectId);
      if (!(await fs.pathExists(suitesDir))) {
        res.status(404).json({ error: '没有找到测试用例' });
        return;
      }
      
      const allRows: TestCaseFile['rows'] = [];
      const entries = await fs.readdir(suitesDir, { withFileTypes: true });
      
      for (const entry of entries) {
        if (entry.isDirectory()) {
          const suitePath = path.join(suitesDir, entry.name, 'cases.json');
          if (await fs.pathExists(suitePath)) {
            const suite: TestCaseFile = await fs.readJson(suitePath);
            allRows.push(...suite.rows);
          }
        }
      }
      
      if (allRows.length === 0) {
        res.status(404).json({ error: '没有找到测试用例' });
        return;
      }
      
      // 使用第一个 suite 的列定义
      const firstSuitePath = path.join(suitesDir, entries[0].name, 'cases.json');
      const firstSuite: TestCaseFile = await fs.readJson(firstSuitePath);
      
      testCaseFile = {
        ...firstSuite,
        name: 'All Test Cases',
        rows: allRows,
      };
    }
    
    // 导出
    if (format === 'csv') {
      const csv = await excelExporter.exportToCSV(testCaseFile);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="${testCaseFile.name}.csv"`);
      res.send(csv);
    } else {
      const buffer = await excelExporter.exportToBuffer(testCaseFile, {
        sheetName: sheetName || testCaseFile.name,
        styles: {
          headerBackgroundColor: '#4472C4',
          headerTextColor: '#FFFFFF',
          headerBold: true,
          borders: true,
        },
      });
      
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="${testCaseFile.name}.xlsx"`);
      res.send(buffer);
    }
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/projects/:projectId/export/:suiteId - 导出特定测试套件
 */
router.get('/:projectId/export/:suiteId', async (req, res, next) => {
  try {
    const { projectId, suiteId } = req.params;
    const { format = 'xlsx', sheetName } = req.query as { 
      format?: 'xlsx' | 'csv';
      sheetName?: string;
    };
    
    // 检查项目是否存在
    const projectDir = path.join(DATA_DIR, projectId);
    if (!(await fs.pathExists(projectDir))) {
      res.status(404).json({ error: '项目不存在' });
      return;
    }
    
    // 获取测试套件
    const suitePath = path.join(getSuitesDir(projectId), suiteId, 'cases.json');
    if (!(await fs.pathExists(suitePath))) {
      res.status(404).json({ error: '测试套件不存在' });
      return;
    }
    
    const testCaseFile: TestCaseFile = await fs.readJson(suitePath);
    
    // 导出
    if (format === 'csv') {
      const csv = await excelExporter.exportToCSV(testCaseFile);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="${testCaseFile.name}.csv"`);
      res.send(csv);
    } else {
      const buffer = await excelExporter.exportToBuffer(testCaseFile, {
        sheetName: sheetName || testCaseFile.name,
        styles: {
          headerBackgroundColor: '#4472C4',
          headerTextColor: '#FFFFFF',
          headerBold: true,
          borders: true,
        },
      });
      
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="${testCaseFile.name}.xlsx"`);
      res.send(buffer);
    }
  } catch (error: any) {
    next(error);
  }
});

export { router as excelRouter };
