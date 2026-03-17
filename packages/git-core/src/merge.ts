/**
 * Three-way merge logic for test case files
 * Supports cell-level conflict detection
 */

import type {
  TestCaseFile,
  RowData,
  CellData,
  ColumnDefinition,
} from '@testcase-manager/shared';

export interface MergeResult {
  success: boolean;
  merged: TestCaseFile;
  conflicts: MergeConflict[];
}

export interface MergeConflict {
  rowId: string;
  columnId?: string;
  cellId?: string;
  baseValue: any;
  localValue: any;
  remoteValue: any;
  resolved?: boolean;
  resolvedValue?: any;
}

/**
 * Three-way merge for TestCaseFile structures
 * @param base - Common ancestor version
 * @param local - Local changes
 * @param remote - Remote changes
 * @returns Merge result with conflicts
 */
export function mergeTestCaseFiles(
  base: TestCaseFile,
  local: TestCaseFile,
  remote: TestCaseFile
): MergeResult {
  const conflicts: MergeConflict[] = [];

  // Merge columns (column definitions should typically be the same)
  const mergedColumns = mergeColumns(base.columns, local.columns, remote.columns);

  // Merge rows with conflict detection
  const { merged: mergedRows, conflicts: rowConflicts } = mergeRows(
    base.rows,
    local.rows,
    remote.rows
  );
  conflicts.push(...rowConflicts);

  // Create merged TestCaseFile
  const merged: TestCaseFile = {
    version: local.version || remote.version || base.version,
    name: local.name || remote.name || base.name,
    description: local.description || remote.description || base.description,
    columns: mergedColumns,
    rows: mergedRows,
    settings: local.settings || remote.settings || base.settings,
    createdAt: base.createdAt,
    updatedAt: new Date().toISOString(),
  };

  return {
    success: conflicts.length === 0,
    merged,
    conflicts,
  };
}

/**
 * Merge column definitions
 */
function mergeColumns(
  base: ColumnDefinition[],
  local: ColumnDefinition[],
  remote: ColumnDefinition[]
): ColumnDefinition[] {
  // Create maps for quick lookup
  const baseMap = new Map(base.map(col => [col.id, col]));
  const localMap = new Map(local.map(col => [col.id, col]));
  const remoteMap = new Map(remote.map(col => [col.id, col]));

  // Get all unique column IDs
  const allIds = new Set([...baseMap.keys(), ...localMap.keys(), ...remoteMap.keys()]);
  const merged: ColumnDefinition[] = [];

  for (const id of allIds) {
    const baseCol = baseMap.get(id);
    const localCol = localMap.get(id);
    const remoteCol = remoteMap.get(id);

    // Case 1: Deleted in both - skip
    if (!localCol && !remoteCol) {
      continue;
    }

    // Case 2: Added in local only
    if (localCol && !remoteCol && !baseCol) {
      merged.push(localCol);
      continue;
    }

    // Case 3: Added in remote only
    if (remoteCol && !localCol && !baseCol) {
      merged.push(remoteCol);
      continue;
    }

    // Case 4: Both have the column - use local as default
    if (localCol) {
      merged.push(localCol);
    } else if (remoteCol) {
      merged.push(remoteCol);
    }
  }

  // Sort by order if available
  return merged.sort((a, b) => (a.order || 0) - (b.order || 0));
}

/**
 * Merge rows with cell-level conflict detection
 */
function mergeRows(
  base: RowData[],
  local: RowData[],
  remote: RowData[]
): { merged: RowData[]; conflicts: MergeConflict[] } {
  const merged: RowData[] = [];
  const conflicts: MergeConflict[] = [];

  // Create maps for quick lookup
  const baseMap = new Map(base.map(row => [row.id, row]));
  const localMap = new Map(local.map(row => [row.id, row]));
  const remoteMap = new Map(remote.map(row => [row.id, row]));

  // Get all unique row IDs
  const allIds = new Set([...baseMap.keys(), ...localMap.keys(), ...remoteMap.keys()]);

  for (const id of allIds) {
    const baseRow = baseMap.get(id);
    const localRow = localMap.get(id);
    const remoteRow = remoteMap.get(id);

    // Case 1: Deleted in both - skip
    if (!localRow && !remoteRow) {
      continue;
    }

    // Case 2: Deleted in local, modified in remote - conflict
    if (!localRow && remoteRow && baseRow) {
      const remoteChanged = !isRowEqual(baseRow, remoteRow);
      if (remoteChanged) {
        conflicts.push({
          rowId: id,
          baseValue: baseRow,
          localValue: null,
          remoteValue: remoteRow,
        });
        // Keep remote version for now
        merged.push(remoteRow);
      }
      continue;
    }

    // Case 3: Deleted in remote, modified in local - conflict
    if (localRow && !remoteRow && baseRow) {
      const localChanged = !isRowEqual(baseRow, localRow);
      if (localChanged) {
        conflicts.push({
          rowId: id,
          baseValue: baseRow,
          localValue: localRow,
          remoteValue: null,
        });
        // Keep local version for now
        merged.push(localRow);
      }
      continue;
    }

    // Case 4: Added in local only
    if (localRow && !remoteRow && !baseRow) {
      merged.push(localRow);
      continue;
    }

    // Case 5: Added in remote only
    if (remoteRow && !localRow && !baseRow) {
      merged.push(remoteRow);
      continue;
    }

    // Case 6: Added in both with same ID - merge cells
    if (localRow && remoteRow && !baseRow) {
      const { merged: mergedRow, conflicts: cellConflicts } = mergeRowCells(
        null,
        localRow,
        remoteRow
      );
      merged.push(mergedRow);
      conflicts.push(...cellConflicts);
      continue;
    }

    // Case 7: Modified in both - three-way merge at cell level
    if (localRow && remoteRow && baseRow) {
      const { merged: mergedRow, conflicts: cellConflicts } = mergeRowCells(
        baseRow,
        localRow,
        remoteRow
      );
      merged.push(mergedRow);
      conflicts.push(...cellConflicts);
    }
  }

  // Sort by row number
  return { merged: merged.sort((a, b) => a.rowNumber - b.rowNumber), conflicts };
}

/**
 * Merge cells of a single row with cell-level conflict detection
 */
function mergeRowCells(
  base: RowData | null,
  local: RowData,
  remote: RowData
): { merged: RowData; conflicts: MergeConflict[] } {
  const merged: RowData = {
    ...local,
    cells: { ...local.cells },
  };
  const conflicts: MergeConflict[] = [];

  // Get all cell keys (column IDs)
  const allKeys = new Set([
    ...Object.keys(local.cells),
    ...Object.keys(remote.cells),
    ...(base ? Object.keys(base.cells) : []),
  ]);

  for (const columnId of allKeys) {
    const baseCell = base?.cells[columnId];
    const localCell = local.cells[columnId];
    const remoteCell = remote.cells[columnId];

    // If local and remote are the same, no conflict
    if (isCellEqual(localCell, remoteCell)) {
      merged.cells[columnId] = localCell;
      continue;
    }

    // If only local changed from base
    if (base && isCellEqual(remoteCell, baseCell)) {
      merged.cells[columnId] = localCell;
      continue;
    }

    // If only remote changed from base
    if (base && isCellEqual(localCell, baseCell)) {
      merged.cells[columnId] = remoteCell;
      continue;
    }

    // Both changed differently - cell-level conflict!
    conflicts.push({
      rowId: local.id,
      columnId,
      baseValue: baseCell?.value,
      localValue: localCell?.value,
      remoteValue: remoteCell?.value,
    });

    // Default to local value
    merged.cells[columnId] = localCell;
  }

  return { merged, conflicts };
}

/**
 * Check if two rows are equal
 */
function isRowEqual(a: RowData, b: RowData): boolean {
  if (a.id !== b.id) return false;
  if (a.rowNumber !== b.rowNumber) return false;
  if (a.parentId !== b.parentId) return false;
  if (a.expanded !== b.expanded) return false;

  // Compare cells
  const aKeys = Object.keys(a.cells);
  const bKeys = Object.keys(b.cells);
  if (aKeys.length !== bKeys.length) return false;

  for (const key of aKeys) {
    if (!isCellEqual(a.cells[key], b.cells[key])) {
      return false;
    }
  }

  return true;
}

/**
 * Check if two cells are equal
 */
function isCellEqual(a: CellData | undefined, b: CellData | undefined): boolean {
  if (a === b) return true;
  if (!a || !b) return false;

  // Compare values
  if (!isEqual(a.value, b.value)) return false;

  // Compare style if present
  if (a.style || b.style) {
    if (!isEqual(a.style, b.style)) return false;
  }

  // Compare formula if present
  if (a.formula !== b.formula) return false;

  // Compare readOnly if present
  if (a.readOnly !== b.readOnly) return false;

  return true;
}

/**
 * Deep equality check
 */
function isEqual(a: any, b: any): boolean {
  if (a === b) return true;
  if (a == null || b == null) return false;
  if (typeof a !== typeof b) return false;

  if (typeof a === 'object') {
    if (Array.isArray(a) !== Array.isArray(b)) return false;
    
    if (Array.isArray(a)) {
      if (a.length !== b.length) return false;
      for (let i = 0; i < a.length; i++) {
        if (!isEqual(a[i], b[i])) return false;
      }
      return true;
    }

    const keysA = Object.keys(a);
    const keysB = Object.keys(b);
    if (keysA.length !== keysB.length) return false;

    for (const key of keysA) {
      if (!keysB.includes(key)) return false;
      if (!isEqual(a[key], b[key])) return false;
    }
    return true;
  }

  return false;
}

/**
 * Resolve a conflict by choosing a value
 */
export function resolveConflict(
  conflict: MergeConflict,
  choice: 'local' | 'remote' | 'custom',
  customValue?: any
): MergeConflict {
  return {
    ...conflict,
    resolved: true,
    resolvedValue: choice === 'custom' ? customValue : 
                   choice === 'local' ? conflict.localValue : 
                   conflict.remoteValue,
  };
}

/**
 * Apply resolved conflicts to merged test case file
 */
export function applyResolvedConflicts(
  merged: TestCaseFile,
  conflicts: MergeConflict[]
): TestCaseFile {
  const result: TestCaseFile = {
    ...merged,
    rows: [...merged.rows],
  };

  for (const conflict of conflicts) {
    if (!conflict.resolved || conflict.resolvedValue === undefined) continue;

    const rowIndex = result.rows.findIndex(row => row.id === conflict.rowId);
    if (rowIndex === -1) continue;

    const row = result.rows[rowIndex];

    if (conflict.columnId) {
      // Cell-level conflict
      result.rows[rowIndex] = {
        ...row,
        cells: {
          ...row.cells,
          [conflict.columnId]: {
            ...row.cells[conflict.columnId],
            value: conflict.resolvedValue,
          },
        },
      };
    } else {
      // Row-level conflict (entire row was deleted/modified)
      if (conflict.resolvedValue === null) {
        // Delete the row
        result.rows = result.rows.filter(r => r.id !== conflict.rowId);
      } else if (typeof conflict.resolvedValue === 'object') {
        // Replace the row
        result.rows[rowIndex] = conflict.resolvedValue;
      }
    }
  }

  return result;
}
