import * as XLSX from 'xlsx';
import { TestCaseFile, RowData, ColumnDefinition, CellData } from '@testcase-manager/shared';
import { ImportOptions, ImportResult, ImportError } from './types';

const DEFAULT_COLUMN_MAPPING: Record<string, string> = {
  'ID': 'id',
  '用例编号': 'id',
  '编号': 'id',
  '标题': 'title',
  '用例标题': 'title',
  '前置条件': 'precondition',
  'Precondition': 'precondition',
  '测试步骤': 'steps',
  '步骤': 'steps',
  'Steps': 'steps',
  '预期结果': 'expectedResult',
  '结果': 'expectedResult',
  'Expected Result': 'expectedResult',
  '优先级': 'priority',
  'Priority': 'priority',
  '状态': 'status',
  'Status': 'status',
  '标签': 'tags',
  'Tags': 'tags',
  '作者': 'author',
  'Author': 'author',
  '创建时间': 'createdAt',
  'Created At': 'createdAt',
  '更新时间': 'updatedAt',
  'Updated At': 'updatedAt',
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
        testCaseFile: this.createEmptyTestCaseFile(),
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
    const rows: RowData[] = [];
    const errors: ImportError[] = [];
    const skippedRows: number[] = [];
    
    for (let i = startRow; i < jsonData.length; i++) {
      const row = jsonData[i];
      
      try {
        const rowData = this.parseRow(row, headers, columnMapping, i);
        if (rowData) {
          rows.push(rowData);
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
    
    // 创建 TestCaseFile
    const testCaseFile = this.createTestCaseFile(rows, headers, columnMapping);
    
    return {
      testCaseFile,
      totalRows: jsonData.length - startRow,
      importedCount: rows.length,
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
  ): Map<number, string> {
    const result = new Map<number, string>();
    
    headers.forEach((header, index) => {
      const field = mapping[header];
      if (field) {
        result.set(index, field);
      }
    });
    
    return result;
  }
  
  private buildDefaultColumnMapping(
    headers: string[]
  ): Map<number, string> {
    return this.buildColumnMapping(headers, DEFAULT_COLUMN_MAPPING);
  }
  
  private parseRow(
    row: any[],
    headers: string[],
    columnMapping: Map<number, string>,
    rowIndex: number
  ): RowData | null {
    // 检查是否为空行
    if (!row || row.every(cell => !cell)) {
      return null;
    }
    
    const cells: Record<string, CellData> = {};
    let hasData = false;
    
    columnMapping.forEach((field, colIndex) => {
      const value = row[colIndex];
      
      if (value !== undefined && value !== null && value !== '') {
        hasData = true;
        switch (field) {
          case 'tags':
            cells[field] = { value: this.parseTags(value) };
            break;
          case 'priority':
            cells[field] = { value: this.parsePriority(value) };
            break;
          case 'status':
            cells[field] = { value: this.parseStatus(value) };
            break;
          default:
            cells[field] = { value: String(value) };
        }
      }
    });
    
    // 如果没有数据，跳过此行
    if (!hasData) {
      return null;
    }
    
    // 验证必填字段
    if (!cells['title']?.value) {
      throw new Error(`第 ${rowIndex + 1} 行缺少标题`);
    }
    
    // 生成 ID（如果没有提供）
    if (!cells['id']?.value) {
      cells['id'] = { value: `TC${Date.now()}_${rowIndex}` };
    }
    
    const now = new Date().toISOString();
    
    return {
      id: cells['id'].value as string,
      cells,
      rowNumber: rowIndex,
      createdAt: now,
      updatedAt: now,
    };
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
  
  private parsePriority(value: any): string {
    const priorityMap: Record<string, string> = {
      'P0': 'critical', '0': 'critical', '高': 'critical', '高优先级': 'critical', 'critical': 'critical',
      'P1': 'high', '1': 'high', '中高': 'high', 'high': 'high',
      'P2': 'medium', '2': 'medium', '中': 'medium', '正常': 'medium', 'medium': 'medium',
      'P3': 'low', '3': 'low', '低': 'low', '低优先级': 'low', 'low': 'low',
    };
    return priorityMap[String(value).toLowerCase()] || 'medium';
  }
  
  private parseStatus(value: any): string {
    const statusMap: Record<string, string> = {
      '草稿': 'draft', 'draft': 'draft',
      '评审中': 'ready', 'review': 'ready', 'pending': 'ready', 'ready': 'ready',
      '已发布': 'ready', 'published': 'ready', 'active': 'ready',
      '已废弃': 'deprecated', 'deprecated': 'deprecated', 'obsolete': 'deprecated',
      '已归档': 'archived', 'archived': 'archived',
    };
    return statusMap[String(value).toLowerCase()] || 'draft';
  }
  
  private createEmptyTestCaseFile(): TestCaseFile {
    const now = new Date().toISOString();
    return {
      version: '1.0',
      name: 'Imported Test Cases',
      description: 'Imported from Excel',
      columns: this.getDefaultColumns(),
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
  }
  
  private createTestCaseFile(
    rows: RowData[],
    headers: string[],
    columnMapping: Map<number, string>
  ): TestCaseFile {
    const now = new Date().toISOString();
    
    // 根据映射创建列定义
    const columns: ColumnDefinition[] = [];
    const mappedFields = new Set(columnMapping.values());
    
    // 添加已映射的列
    columnMapping.forEach((field, colIndex) => {
      const header = headers[colIndex];
      columns.push({
        id: field,
        name: header || field,
        type: this.getColumnType(field),
        required: field === 'id' || field === 'title',
        width: this.getDefaultColumnWidth(field),
        order: colIndex,
        visible: true,
        options: this.getColumnOptions(field),
      });
    });
    
    // 添加默认列（如果未映射）
    const defaultColumns = this.getDefaultColumns();
    defaultColumns.forEach(col => {
      if (!mappedFields.has(col.id)) {
        columns.push({
          ...col,
          order: columns.length,
        });
      }
    });
    
    // 按 order 排序
    columns.sort((a, b) => (a.order || 0) - (b.order || 0));
    
    return {
      version: '1.0',
      name: 'Imported Test Cases',
      description: 'Imported from Excel',
      columns,
      rows,
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
  }
  
  private getDefaultColumns(): ColumnDefinition[] {
    return [
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
        id: 'precondition',
        name: 'Precondition',
        type: 'text',
        width: 250,
        order: 2,
        visible: true,
      },
      {
        id: 'steps',
        name: 'Steps',
        type: 'text',
        width: 350,
        order: 3,
        visible: true,
      },
      {
        id: 'expectedResult',
        name: 'Expected Result',
        type: 'text',
        width: 300,
        order: 4,
        visible: true,
      },
      {
        id: 'priority',
        name: 'Priority',
        type: 'select',
        options: ['low', 'medium', 'high', 'critical'],
        width: 120,
        order: 5,
        visible: true,
      },
      {
        id: 'status',
        name: 'Status',
        type: 'select',
        options: ['draft', 'ready', 'deprecated', 'archived'],
        width: 120,
        order: 6,
        visible: true,
      },
      {
        id: 'tags',
        name: 'Tags',
        type: 'tags',
        width: 200,
        order: 7,
        visible: true,
      },
    ];
  }
  
  private getColumnType(field: string): ColumnDefinition['type'] {
    switch (field) {
      case 'priority':
      case 'status':
        return 'select';
      case 'tags':
        return 'tags';
      default:
        return 'text';
    }
  }
  
  private getDefaultColumnWidth(field: string): number {
    switch (field) {
      case 'id':
        return 100;
      case 'title':
        return 300;
      case 'steps':
      case 'expectedResult':
        return 350;
      case 'precondition':
        return 250;
      case 'priority':
      case 'status':
        return 120;
      case 'tags':
        return 200;
      default:
        return 150;
    }
  }
  
  private getColumnOptions(field: string): string[] | undefined {
    switch (field) {
      case 'priority':
        return ['low', 'medium', 'high', 'critical'];
      case 'status':
        return ['draft', 'ready', 'deprecated', 'archived'];
      default:
        return undefined;
    }
  }
}

export const excelImporter = new ExcelImporter();
