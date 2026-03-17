/**
 * TestCase Manager - Core Type Definitions
 * 
 * This module defines all types related to test cases, including:
 * - Test case metadata and structure
 * - Column definitions for the spreadsheet-like interface
 * - Cell and row data structures
 * - Test suites and projects
 */

/**
 * Supported column types for test case fields
 */
export type ColumnType = 'text' | 'number' | 'select' | 'tags' | 'date';

/**
 * Cell styling options
 */
export interface CellStyle {
  /** Background color in hex format */
  backgroundColor?: string;
  /** Text color in hex format */
  textColor?: string;
  /** Font weight */
  fontWeight?: 'normal' | 'bold';
  /** Text alignment */
  textAlign?: 'left' | 'center' | 'right';
  /** Whether the cell is bold */
  bold?: boolean;
  /** Whether the cell is italic */
  italic?: boolean;
}

/**
 * Individual cell data structure
 */
export interface CellData {
  /** The value stored in the cell */
  value: string | number | string[] | null;
  /** Optional styling for the cell */
  style?: CellStyle;
  /** Optional formula for computed cells */
  formula?: string;
  /** Whether the cell is read-only */
  readOnly?: boolean;
  /** Cell comments/notes */
  comment?: string;
}

/**
 * Column definition for test case fields
 */
export interface ColumnDefinition {
  /** Unique identifier for the column */
  id: string;
  /** Display name of the column */
  name: string;
  /** Data type of the column */
  type: ColumnType;
  /** Whether the column is required */
  required?: boolean;
  /** Default value for the column */
  defaultValue?: string | number | string[];
  /** Width of the column in pixels */
  width?: number;
  /** For 'select' type: available options */
  options?: string[];
  /** For 'select' type: whether multiple selection is allowed */
  multiSelect?: boolean;
  /** Description/help text for the column */
  description?: string;
  /** Display order of the column */
  order?: number;
  /** Whether the column is visible */
  visible?: boolean;
}

/**
 * Row data structure representing a single test case
 */
export interface RowData {
  /** Unique identifier for the row */
  id: string;
  /** Cell data keyed by column ID */
  cells: Record<string, CellData>;
  /** Row number for display */
  rowNumber: number;
  /** Whether the row is expanded (for grouping) */
  expanded?: boolean;
  /** Parent row ID (for hierarchical structures) */
  parentId?: string | null;
  /** Creation timestamp */
  createdAt: string;
  /** Last update timestamp */
  updatedAt: string;
}

/**
 * Test case metadata
 */
export interface TestCaseMetadata {
  /** Unique identifier */
  id: string;
  /** Human-readable identifier (e.g., TC-001) */
  identifier: string;
  /** Test case title */
  title: string;
  /** Test case description */
  description?: string;
  /** Priority level */
  priority: 'low' | 'medium' | 'high' | 'critical';
  /** Current status */
  status: 'draft' | 'ready' | 'deprecated' | 'archived';
  /** Assigned tester */
  assignee?: string;
  /** Tags/labels */
  tags: string[];
  /** Creation timestamp */
  createdAt: string;
  /** Last update timestamp */
  updatedAt: string;
  /** Creator user ID */
  createdBy: string;
  /** Last modifier user ID */
  updatedBy: string;
}

/**
 * Test case file settings
 */
export interface TestCaseSettings {
  /** Auto-save interval in seconds (0 = disabled) */
  autoSaveInterval: number;
  /** Default column width */
  defaultColumnWidth: number;
  /** Enable row numbers */
  showRowNumbers: boolean;
  /** Enable grid lines */
  showGridLines: boolean;
  /** Timezone for date columns */
  timezone: string;
  /** Date format string */
  dateFormat: string;
  /** Enable version control */
  versionControl: boolean;
  /** Conflict resolution strategy */
  conflictResolution: 'manual' | 'auto-merge' | 'last-write-wins';
}

/**
 * Complete test case file structure
 */
export interface TestCaseFile {
  /** File format version */
  version: string;
  /** File name */
  name: string;
  /** File description */
  description?: string;
  /** Column definitions */
  columns: ColumnDefinition[];
  /** Row data */
  rows: RowData[];
  /** File settings */
  settings: TestCaseSettings;
  /** Creation timestamp */
  createdAt: string;
  /** Last update timestamp */
  updatedAt: string;
}

/**
 * Test suite containing multiple test cases
 */
export interface TestSuite {
  /** Unique identifier */
  id: string;
  /** Suite name */
  name: string;
  /** Suite description */
  description?: string;
  /** Associated test case file IDs */
  testCaseFileIds: string[];
  /** Execution order of test cases */
  executionOrder?: string[];
  /** Suite-level tags */
  tags: string[];
  /** Creation timestamp */
  createdAt: string;
  /** Last update timestamp */
  updatedAt: string;
  /** Creator user ID */
  createdBy: string;
}

/**
 * Project containing multiple test suites and files
 */
export interface Project {
  /** Unique identifier */
  id: string;
  /** Project name */
  name: string;
  /** Project description */
  description?: string;
  /** Associated test suite IDs */
  testSuiteIds: string[];
  /** Associated test case file IDs */
  testCaseFileIds: string[];
  /** Project members/user IDs */
  members: string[];
  /** Project owner */
  ownerId: string;
  /** Project status */
  status: 'active' | 'archived' | 'deleted';
  /** Git repository URL (optional) */
  gitRepositoryUrl?: string;
  /** Creation timestamp */
  createdAt: string;
  /** Last update timestamp */
  updatedAt: string;
}
