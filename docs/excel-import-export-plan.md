# Excel 导入导出实施计划 - 阶段 6

> **任务:** 为 TestCase Manager 添加 Excel 导入导出功能，支持 .xlsx 格式的测试用例数据交换。

## 目标

实现测试用例与 Excel 文件之间的双向转换，使用户能够：
- 将现有 Excel 测试用例导入到系统中
- 将系统中的测试用例导出为 Excel 文件
- 保持数据格式和样式的兼容性

---

## 技术方案

使用 **SheetJS (xlsx)** 库处理 Excel 文件：
- 读取 .xlsx 文件并解析为 JSON
- 将测试用例数据写入 .xlsx 文件
- 支持样式、列宽、下拉选项等高级特性

---

## 任务清单

### 任务 6.1: 创建 Excel 核心库 (packages/excel-core)

**文件:**
- 创建: `packages/excel-core/package.json`
- 创建: `packages/excel-core/tsconfig.json`
- 创建: `packages/excel-core/src/index.ts`
- 创建: `packages/excel-core/src/importer.ts`
- 创建: `packages/excel-core/src/exporter.ts`
- 创建: `packages/excel-core/src/types.ts`

#### 步骤 1: 创建 package.json

```json
{
  "name": "@testcase/excel-core",
  "version": "0.1.0",
  "main": "./dist/index.js",
  "types": "./dist/index.d.ts",
  "scripts": {
    "build": "tsc",
    "dev": "tsc --watch",
    "test": "jest"
  },
  "dependencies": {
    "@testcase/shared": "0.1.0",
    "xlsx": "^0.18.5"
  },
  "devDependencies": {
    "@types/jest": "^29.5.0",
    "jest": "^29.7.0",
    "ts-jest": "^29.1.0",
    "typescript": "^5.3.0"
  }
}
```

#### 步骤 2: 创建 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "CommonJS",
    "lib": ["ES2020"],
    "declaration": true,
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "outDir": "./dist",
    "rootDir": "./src"
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

#### 步骤 3: 创建类型定义 (src/types.ts)

```typescript
import { TestCase } from '@testcase/shared';

export interface ImportOptions {
  /** 起始行（0-based，默认 1，跳过表头） */
  startRow?: number;
  /** 工作表名称或索引 */
  sheet?: string | number;
  /** 列映射配置 */
  columnMapping?: Record<string, string>;
}

export interface ExportOptions {
  /** 工作表名称 */
  sheetName?: string;
  /** 包含表头 */
  includeHeader?: boolean;
  /** 列宽配置 */
  columnWidths?: Record<string, number>;
  /** 样式配置 */
  styles?: ExportStyles;
}

export interface ExportStyles {
  /** 表头背景色 */
  headerBackgroundColor?: string;
  /** 表头文字颜色 */
  headerTextColor?: string;
  /** 表头字体加粗 */
  headerBold?: boolean;
  /** 边框 */
  borders?: boolean;
}

export interface ImportResult {
  /** 导入的测试用例 */
  testCases: TestCase[];
  /** 总行数 */
  totalRows: number;
  /** 成功导入数 */
  importedCount: number;
  /** 跳过的行 */
  skippedRows: number[];
  /** 错误信息 */
  errors: ImportError[];
}

export interface ImportError {
  /** 行号 */
  row: number;
  /** 错误信息 */
  message: string;
  /** 原始数据 */
  rawData?: any;
}

export interface ColumnMapping {
  /** Excel 列名 */
  excelColumn: string;
  /** 测试用例字段 */
  testCaseField: keyof TestCase;
  /** 是否必填 */
  required?: boolean;
  /** 数据转换函数 */
  transform?: (value: any) => any;
}
```

#### 步骤 4: 创建导入器 (src/importer.ts)

```typescript
import * as XLSX from 'xlsx';
import { TestCase } from '@testcase/shared';
import { ImportOptions, ImportResult, ImportError } from './types';

const DEFAULT_COLUMN_MAPPING: Record<string, keyof TestCase> = {
  'ID': 'id',
  '用例编号': 'id',
  '标题': 'title',
  '用例标题': 'title',
  '前置条件': 'precondition',
  '测试步骤': 'steps',
  '步骤': 'steps',
  '预期结果': 'expectedResult',
  '结果': 'expectedResult',
  '优先级': 'priority',
  '状态': 'status',
  '标签': 'tags',
  '作者': 'author',
};

export class ExcelImporter {
  /**
   * 从 Excel 文件导入测试用例
   */
  async importFromBuffer(
    buffer: Buffer,
    options: ImportOptions = {}
  ): Promise<ImportResult> {
    const workbook = XLSX.read(buffer, { type: 'buffer' });
    
    // 获取工作表
    const sheetName = this.getSheetName(workbook, options.sheet);
    const worksheet = workbook.Sheets[sheetName];
    
    if (!worksheet) {
      throw new Error(`工作表 "${sheetName}" 不存在`);
    }
    
    // 转换为 JSON
    const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 }) as any[][];
    
    if (jsonData.length === 0) {
      return {
        testCases: [],
        totalRows: 0,
        importedCount: 0,
        skippedRows: [],
        errors: [],
      };
    }
    
    // 解析表头
    const headers = jsonData[0] as string[];
    const startRow = options.startRow ?? 1;
    
    // 构建列映射
    const columnMapping = options.columnMapping 
      ? this.buildColumnMapping(headers, options.columnMapping)
      : this.buildDefaultColumnMapping(headers);
    
    // 解析数据行
    const testCases: TestCase[] = [];
    const errors: ImportError[] = [];
    const skippedRows: number[] = [];
    
    for (let i = startRow; i < jsonData.length; i++) {
      const row = jsonData[i];
      
      try {
        const testCase = this.parseRow(row, headers, columnMapping, i);
        if (testCase) {
          testCases.push(testCase);
        } else {
          skippedRows.push(i);
        }
      } catch (error: any) {
        errors.push({
          row: i,
          message: error.message,
          rawData: row,
        });
      }
    }
    
    return {
      testCases,
      totalRows: jsonData.length - startRow,
      importedCount: testCases.length,
      skippedRows,
      errors,
    };
  }
  
  /**
   * 从文件路径导入
   */
  async importFromFile(
    filePath: string,
    options?: ImportOptions
  ): Promise<ImportResult> {
    const fs = await import('fs');
    const buffer = fs.readFileSync(filePath);
    return this.importFromBuffer(buffer, options);
  }
  
  private getSheetName(
    workbook: XLSX.WorkBook,
    sheet?: string | number
  ): string {
    if (typeof sheet === 'string') {
      return sheet;
    }
    if (typeof sheet === 'number') {
      return workbook.SheetNames[sheet] || workbook.SheetNames[0];
    }
    return workbook.SheetNames[0];
  }
  
  private buildColumnMapping(
    headers: string[],
    mapping: Record<string, string>
  ): Map<number, keyof TestCase> {
    const result = new Map<number, keyof TestCase>();
    
    headers.forEach((header, index) => {
      const field = mapping[header];
      if (field) {
        result.set(index, field as keyof TestCase);
      }
    });
    
    return result;
  }
  
  private buildDefaultColumnMapping(
    headers: string[]
  ): Map<number, keyof TestCase> {
    return this.buildColumnMapping(headers, DEFAULT_COLUMN_MAPPING);
  }
  
  private parseRow(
    row: any[],
    headers: string[],
    columnMapping: Map<number, keyof TestCase>,
    rowIndex: number
  ): TestCase | null {
    // 检查是否为空行
    if (!row || row.every(cell => !cell)) {
      return null;
    }
    
    const testCase: Partial<TestCase> = {
      priority: 'P2',
      status: '草稿',
      tags: [],
    };
    
    columnMapping.forEach((field, colIndex) => {
      const value = row[colIndex];
      
      if (value !== undefined && value !== null && value !== '') {
        switch (field) {
          case 'tags':
            testCase[field] = this.parseTags(value);
            break;
          case 'priority':
            testCase[field] = this.parsePriority(value);
            break;
          case 'status':
            testCase[field] = this.parseStatus(value);
            break;
          default:
            (testCase as any)[field] = String(value);
        }
      }
    });
    
    // 验证必填字段
    if (!testCase.title) {
      throw new Error(`第 ${rowIndex + 1} 行缺少标题`);
    }
    
    // 生成 ID（如果没有提供）
    if (!testCase.id) {
      testCase.id = `TC${Date.now()}_${rowIndex}`;
    }
    
    return testCase as TestCase;
  }
  
  private parseTags(value: any): string[] {
    if (Array.isArray(value)) {
      return value.map(String);
    }
    if (typeof value === 'string') {
      return value.split(/[,，;；]/).map(t => t.trim()).filter(Boolean);
    }
    return [];
  }
  
  private parsePriority(value: any): TestCase['priority'] {
    const priorityMap: Record<string, TestCase['priority']> = {
      'P0': 'P0', '0': 'P0', '高': 'P0', '高优先级': 'P0',
      'P1': 'P1', '1': 'P1', '中高': 'P1',
      'P2': 'P2', '2': 'P2', '中': 'P2', '正常': 'P2',
      'P3': 'P3', '3': 'P3', '低': 'P3', '低优先级': 'P3',
    };
    return priorityMap[String(value).toUpperCase()] || 'P2';
  }
  
  private parseStatus(value: any): TestCase['status'] {
    const statusMap: Record<string, TestCase['status']> = {
      '草稿': '草稿', 'draft': '草稿',
      '评审中': '评审中', 'review': '评审中', 'pending': '评审中',
      '已发布': '已发布', 'published': '已发布', 'active': '已发布',
      '已废弃': '已废弃', 'deprecated': '已废弃', 'obsolete': '已废弃',
    };
    return statusMap[String(value).toLowerCase()] || '草稿';
  }
}

export const excelImporter = new ExcelImporter();
```

#### 步骤 5: 创建导出器 (src/exporter.ts)

```typescript
import * as XLSX from 'xlsx';
import { TestCase } from '@testcase/shared';
import { ExportOptions } from './types';

const DEFAULT_HEADERS = [
  { key: 'id', title: '用例编号', width: 15 },
  { key: 'title', title: '用例标题', width: 30 },
  { key: 'precondition', title: '前置条件', width: 25 },
  { key: 'steps', title: '测试步骤', width: 35 },
  { key: 'expectedResult', title: '预期结果', width: 30 },
  { key: 'priority', title: '优先级', width: 10 },
  { key: 'status', title: '状态', width: 12 },
  { key: 'tags', title: '标签', width: 20 },
  { key: 'author', title: '作者', width: 12 },
];

export class ExcelExporter {
  /**
   * 导出测试用例到 Excel Buffer
   */
  async exportToBuffer(
    testCases: TestCase[],
    options: ExportOptions = {}
  ): Promise<Buffer> {
    const workbook = XLSX.utils.book_new();
    
    // 准备数据
    const data = this.prepareData(testCases, options.includeHeader !== false);
    
    // 创建工作表
    const worksheet = XLSX.utils.aoa_to_sheet(data);
    
    // 设置列宽
    this.setColumnWidths(worksheet, options.columnWidths);
    
    // 应用样式
    if (options.styles) {
      this.applyStyles(worksheet, data.length, options.styles);
    }
    
    // 添加工作表到工作簿
    XLSX.utils.book_append_sheet(
      workbook,
      worksheet,
      options.sheetName || '测试用例'
    );
    
    // 生成 Buffer
    return XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' });
  }
  
  /**
   * 导出到文件
   */
  async exportToFile(
    testCases: TestCase[],
    filePath: string,
    options?: ExportOptions
  ): Promise<void> {
    const fs = await import('fs');
    const buffer = await this.exportToBuffer(testCases, options);
    fs.writeFileSync(filePath, buffer);
  }
  
  /**
   * 导出为 CSV
   */
  async exportToCSV(testCases: TestCase[]): Promise<string> {
    const workbook = XLSX.utils.book_new();
    const data = this.prepareData(testCases, true);
    const worksheet = XLSX.utils.aoa_to_sheet(data);
    XLSX.utils.book_append_sheet(workbook, worksheet, '测试用例');
    
    return XLSX.write(workbook, { type: 'string', bookType: 'csv' });
  }
  
  private prepareData(testCases: TestCase[], includeHeader: boolean): any[][] {
    const data: any[][] = [];
    
    // 添加表头
    if (includeHeader) {
      data.push(DEFAULT_HEADERS.map(h => h.title));
    }
    
    // 添加数据行
    testCases.forEach(tc => {
      data.push([
        tc.id,
        tc.title,
        tc.precondition || '',
        tc.steps || '',
        tc.expectedResult || '',
        tc.priority,
        tc.status,
        tc.tags?.join(', ') || '',
        tc.author || '',
      ]);
    });
    
    return data;
  }
  
  private setColumnWidths(
    worksheet: XLSX.WorkSheet,
    customWidths?: Record<string, number>
  ): void {
    const widths = DEFAULT_HEADERS.map(h => ({
      wch: customWidths?.[h.key] || h.width,
    }));
    
    worksheet['!cols'] = widths;
  }
  
  private applyStyles(
    worksheet: XLSX.WorkSheet,
    rowCount: number,
    styles: ExportOptions['styles']
  ): void {
    if (!styles) return;
    
    // 表头样式（第一行）
    const headerRange = XLSX.utils.decode_range(worksheet['!ref'] || 'A1');
    
    for (let col = headerRange.s.c; col <= headerRange.e.c; col++) {
      const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
      const cell = worksheet[cellAddress];
      
      if (cell) {
        cell.s = cell.s || {};
        
        if (styles.headerBackgroundColor) {
          cell.s.fill = {
            fgColor: { rgb: styles.headerBackgroundColor.replace('#', '') },
            patternType: 'solid',
          };
        }
        
        if (styles.headerTextColor) {
          cell.s.font = cell.s.font || {};
          cell.s.font.color = { rgb: styles.headerTextColor.replace('#', '') };
        }
        
        if (styles.headerBold) {
          cell.s.font = cell.s.font || {};
          cell.s.font.bold = true;
        }
      }
    }
  }
}

export const excelExporter = new ExcelExporter();
```

#### 步骤 6: 创建索引文件 (src/index.ts)

```typescript
export { ExcelImporter, excelImporter } from './importer';
export { ExcelExporter, excelExporter } from './exporter';
export * from './types';
```

#### 步骤 7: 构建并提交

```bash
cd packages/excel-core
npm install
cd ../..
git add .
git commit -m "feat(excel-core): implement Excel import/export functionality"
```

---

### 任务 6.2: 后端 API 集成

**文件:**
- 修改: `services/api/src/routes.ts` 或创建新路由文件

#### 步骤 1: 添加 Excel 导入导出路由

```typescript
// services/api/src/routes/excel.ts
import { Router } from 'express';
import multer from 'multer';
import { excelImporter, excelExporter } from '@testcase/excel-core';
import { projectService } from '../services/projectService';

const router = Router();
const upload = multer({ storage: multer.memoryStorage() });

// 导入 Excel
router.post(
  '/projects/:projectId/import',
  upload.single('file'),
  async (req, res) => {
    try {
      const { projectId } = req.params;
      const file = req.file;
      
      if (!file) {
        return res.status(400).json({ error: '请上传文件' });
      }
      
      // 检查文件类型
      if (!file.originalname.match(/\.(xlsx|xls)$/i)) {
        return res.status(400).json({ error: '只支持 .xlsx 或 .xls 文件' });
      }
      
      // 导入数据
      const result = await excelImporter.importFromBuffer(file.buffer, {
        columnMapping: req.body.columnMapping,
      });
      
      // 保存到项目
      if (result.testCases.length > 0) {
        await projectService.addTestCases(projectId, result.testCases);
      }
      
      res.json({
        success: true,
        imported: result.importedCount,
        total: result.totalRows,
        errors: result.errors,
      });
    } catch (error: any) {
      res.status(500).json({ error: error.message });
    }
  }
);

// 导出 Excel
router.get('/projects/:projectId/export', async (req, res) => {
  try {
    const { projectId } = req.params;
    const { format = 'xlsx' } = req.query;
    
    // 获取测试用例
    const testCases = await projectService.getTestCases(projectId);
    
    if (format === 'csv') {
      const csv = await excelExporter.exportToCSV(testCases);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="testcases.csv"`);
      res.send(csv);
    } else {
      const buffer = await excelExporter.exportToBuffer(testCases, {
        sheetName: '测试用例',
        styles: {
          headerBackgroundColor: '#4472C4',
          headerTextColor: '#FFFFFF',
          headerBold: true,
        },
      });
      
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="testcases.xlsx"`);
      res.send(buffer);
    }
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default router;
```

---

### 任务 6.3: Web 前端集成

**文件:**
- 创建: `apps/web/src/components/ImportExport.tsx`
- 修改: `apps/web/src/App.tsx` 添加导入导出按钮

#### 步骤 1: 创建导入导出组件

```typescript
// apps/web/src/components/ImportExport.tsx
import { useState, useRef } from 'react';
import { projectsApi } from '../api/projects';

interface ImportExportProps {
  projectId: string;
  onImportSuccess?: () => void;
}

export function ImportExport({ projectId, onImportSuccess }: ImportExportProps) {
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setImporting(true);
    try {
      const result = await projectsApi.importExcel(projectId, file);
      
      if (result.errors?.length > 0) {
        alert(`导入完成，但存在 ${result.errors.length} 个错误`);
      } else {
        alert(`成功导入 ${result.imported} 条测试用例`);
      }
      
      onImportSuccess?.();
    } catch (error: any) {
      alert(`导入失败: ${error.message}`);
    } finally {
      setImporting(false);
      // 重置文件输入
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleExport = async (format: 'xlsx' | 'csv' = 'xlsx') => {
    setExporting(true);
    try {
      await projectsApi.exportExcel(projectId, format);
    } catch (error: any) {
      alert(`导出失败: ${error.message}`);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="import-export-toolbar">
      <input
        type="file"
        ref={fileInputRef}
        accept=".xlsx,.xls"
        onChange={handleImport}
        style={{ display: 'none' }}
      />
      
      <button
        onClick={() => fileInputRef.current?.click()}
        disabled={importing}
        className="btn-import"
      >
        {importing ? '导入中...' : '导入 Excel'}
      </button>
      
      <button
        onClick={() => handleExport('xlsx')}
        disabled={exporting}
        className="btn-export"
      >
        {exporting ? '导出中...' : '导出 Excel'}
      </button>
      
      <button
        onClick={() => handleExport('csv')}
        disabled={exporting}
        className="btn-export-csv"
      >
        导出 CSV
      </button>
    </div>
  );
}
```

---

## 提交

```bash
git add .
git commit -m "feat(excel): add Excel import/export functionality to API and web"
```

---

**计划完成！** 准备开始实施。
