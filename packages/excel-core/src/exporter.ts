import * as XLSX from 'xlsx';
import { TestCaseFile, RowData, ColumnDefinition } from '@testcase-manager/shared';
import { ExportOptions, ExportFormat } from './types';

interface HeaderConfig {
  key: string;
  title: string;
  width: number;
}

const DEFAULT_HEADERS: HeaderConfig[] = [
  { key: 'id', title: '用例编号', width: 15 },
  { key: 'title', title: '用例标题', width: 30 },
  { key: 'precondition', title: '前置条件', width: 25 },
  { key: 'steps', title: '测试步骤', width: 35 },
  { key: 'expectedResult', title: '预期结果', width: 30 },
  { key: 'priority', title: '优先级', width: 10 },
  { key: 'status', title: '状态', width: 12 },
  { key: 'tags', title: '标签', width: 20 },
];

export class ExcelExporter {
  /**
   * 导出测试用例到 Excel Buffer
   */
  async exportToBuffer(
    testCaseFile: TestCaseFile,
    options: ExportOptions = {}
  ): Promise<Buffer> {
    const workbook = XLSX.utils.book_new();
    
    // 准备数据
    const data = this.prepareData(testCaseFile, options.includeHeader !== false);
    
    // 创建工作表
    const worksheet = XLSX.utils.aoa_to_sheet(data);
    
    // 设置列宽
    this.setColumnWidths(worksheet, testCaseFile.columns, options.columnWidths);
    
    // 应用样式
    if (options.styles) {
      this.applyStyles(worksheet, data.length, options.styles);
    }
    
    // 添加工作表到工作簿
    XLSX.utils.book_append_sheet(
      workbook,
      worksheet,
      options.sheetName || testCaseFile.name || '测试用例'
    );
    
    // 生成 Buffer
    return XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' });
  }
  
  /**
   * 导出到文件
   */
  async exportToFile(
    testCaseFile: TestCaseFile,
    filePath: string,
    options?: ExportOptions
  ): Promise<void> {
    const fs = await import('fs');
    const buffer = await this.exportToBuffer(testCaseFile, options);
    fs.writeFileSync(filePath, buffer);
  }
  
  /**
   * 导出为 CSV
   */
  async exportToCSV(testCaseFile: TestCaseFile): Promise<string> {
    const workbook = XLSX.utils.book_new();
    const data = this.prepareData(testCaseFile, true);
    const worksheet = XLSX.utils.aoa_to_sheet(data);
    XLSX.utils.book_append_sheet(workbook, worksheet, '测试用例');
    
    return XLSX.write(workbook, { type: 'string', bookType: 'csv' });
  }
  
  /**
   * 导出为指定格式
   */
  async export(
    testCaseFile: TestCaseFile,
    format: ExportFormat,
    options?: ExportOptions
  ): Promise<Buffer | string> {
    if (format === 'csv') {
      return this.exportToCSV(testCaseFile);
    }
    return this.exportToBuffer(testCaseFile, options);
  }
  
  private prepareData(testCaseFile: TestCaseFile, includeHeader: boolean): any[][] {
    const data: any[][] = [];
    const columns = testCaseFile.columns || DEFAULT_HEADERS.map(h => ({
      id: h.key,
      name: h.title,
      type: 'text' as const,
      width: h.width,
    }));
    
    // 确定要导出的列顺序
    const exportColumns = this.getExportColumns(columns);
    
    // 添加表头
    if (includeHeader) {
      data.push(exportColumns.map(col => col.name));
    }
    
    // 添加数据行
    testCaseFile.rows.forEach(row => {
      const rowData = exportColumns.map(col => {
        const cell = row.cells[col.id];
        if (!cell) return '';
        
        const value = cell.value;
        
        // 处理数组类型（如 tags）
        if (Array.isArray(value)) {
          return value.join(', ');
        }
        
        return value ?? '';
      });
      data.push(rowData);
    });
    
    return data;
  }
  
  private getExportColumns(columns: ColumnDefinition[]): ColumnDefinition[] {
    // 优先使用预定义的顺序
    const priorityOrder = ['id', 'title', 'precondition', 'steps', 'expectedResult', 'priority', 'status', 'tags', 'author', 'createdAt', 'updatedAt'];
    
    const sortedColumns = [...columns].sort((a, b) => {
      const aIndex = priorityOrder.indexOf(a.id);
      const bIndex = priorityOrder.indexOf(b.id);
      
      if (aIndex !== -1 && bIndex !== -1) {
        return aIndex - bIndex;
      }
      if (aIndex !== -1) return -1;
      if (bIndex !== -1) return 1;
      return (a.order || 0) - (b.order || 0);
    });
    
    return sortedColumns.filter(col => col.visible !== false);
  }
  
  private setColumnWidths(
    worksheet: XLSX.WorkSheet,
    columns: ColumnDefinition[],
    customWidths?: Record<string, number>
  ): void {
    const exportColumns = this.getExportColumns(columns);
    
    const widths = exportColumns.map(col => ({
      wch: customWidths?.[col.id] || col.width || this.getDefaultWidth(col.id),
    }));
    
    worksheet['!cols'] = widths;
  }
  
  private getDefaultWidth(field: string): number {
    const widthMap: Record<string, number> = {
      'id': 15,
      'title': 30,
      'precondition': 25,
      'steps': 35,
      'expectedResult': 30,
      'priority': 10,
      'status': 12,
      'tags': 20,
      'author': 12,
      'createdAt': 20,
      'updatedAt': 20,
    };
    return widthMap[field] || 15;
  }
  
  private applyStyles(
    worksheet: XLSX.WorkSheet,
    rowCount: number,
    styles: ExportOptions['styles']
  ): void {
    if (!styles) return;
    
    // 获取工作表范围
    const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1');
    
    // 表头样式（第一行）
    for (let col = range.s.c; col <= range.e.c; col++) {
      const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
      const cell = worksheet[cellAddress];
      
      if (cell) {
        cell.s = cell.s || {};
        
        // 背景色
        if (styles.headerBackgroundColor) {
          cell.s.fill = {
            fgColor: { rgb: styles.headerBackgroundColor.replace('#', '') },
            patternType: 'solid',
          };
        }
        
        // 文字颜色
        if (styles.headerTextColor) {
          cell.s.font = cell.s.font || {};
          cell.s.font.color = { rgb: styles.headerTextColor.replace('#', '') };
        }
        
        // 加粗
        if (styles.headerBold) {
          cell.s.font = cell.s.font || {};
          cell.s.font.bold = true;
        }
        
        // 边框
        if (styles.borders) {
          cell.s.border = {
            top: { style: 'thin', color: { rgb: '000000' } },
            bottom: { style: 'thin', color: { rgb: '000000' } },
            left: { style: 'thin', color: { rgb: '000000' } },
            right: { style: 'thin', color: { rgb: '000000' } },
          };
        }
      }
    }
    
    // 数据行边框
    if (styles.borders) {
      for (let row = 1; row < rowCount; row++) {
        for (let col = range.s.c; col <= range.e.c; col++) {
          const cellAddress = XLSX.utils.encode_cell({ r: row, c: col });
          const cell = worksheet[cellAddress];
          
          if (cell) {
            cell.s = cell.s || {};
            cell.s.border = {
              top: { style: 'thin', color: { rgb: 'CCCCCC' } },
              bottom: { style: 'thin', color: { rgb: 'CCCCCC' } },
              left: { style: 'thin', color: { rgb: 'CCCCCC' } },
              right: { style: 'thin', color: { rgb: 'CCCCCC' } },
            };
          }
        }
      }
    }
  }
}

export const excelExporter = new ExcelExporter();
