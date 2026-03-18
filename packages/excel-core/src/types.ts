import { TestCaseFile, RowData, ColumnDefinition, CellData } from '@testcase-manager/shared';

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
  /** 导入的测试用例文件 */
  testCaseFile: TestCaseFile;
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
  testCaseField: string;
  /** 是否必填 */
  required?: boolean;
  /** 数据转换函数 */
  transform?: (value: any) => any;
}

/** 支持的导出格式 */
export type ExportFormat = 'xlsx' | 'csv';
