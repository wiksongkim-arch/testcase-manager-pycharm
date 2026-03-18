/**
 * Unit tests for TestCaseFile merge logic
 */

import {
  mergeTestCaseFiles,
  resolveConflict,
  applyResolvedConflicts,
  MergeConflict,
} from '../merge';
import type { TestCaseFile, RowData, CellData, ColumnDefinition } from '@testcase-manager/shared';

// Helper function to create a basic TestCaseFile
function createTestFile(overrides: Partial<TestCaseFile> = {}): TestCaseFile {
  const now = new Date().toISOString();
  return {
    version: '1.0',
    name: 'Test File',
    description: 'Test description',
    columns: [
      { id: 'col1', name: 'Title', type: 'text', order: 0 },
      { id: 'col2', name: 'Priority', type: 'select', order: 1 },
      { id: 'col3', name: 'Status', type: 'select', order: 2 },
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
    ...overrides,
  };
}

// Helper function to create a row
function createRow(id: string, rowNumber: number, cells: Record<string, CellData> = {}): RowData {
  const now = new Date().toISOString();
  return {
    id,
    rowNumber,
    cells: {
      col1: { value: `Test ${id}` },
      col2: { value: 'medium' },
      col3: { value: 'draft' },
      ...cells,
    },
    createdAt: now,
    updatedAt: now,
  };
}

describe('mergeTestCaseFiles', () => {
  describe('1. No conflict - simple merge (both modify different fields)', () => {
    it('should merge when local and remote modify different cells', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Original Title' },
            col2: { value: 'medium' },
          }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Local Title' },
            col2: { value: 'medium' },
          }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Original Title' },
            col2: { value: 'high' },
          }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.conflicts).toHaveLength(0);
      expect(result.merged.rows).toHaveLength(1);
      expect(result.merged.rows[0].cells.col1.value).toBe('Local Title');
      expect(result.merged.rows[0].cells.col2.value).toBe('high');
    });

    it('should merge when local modifies one row and remote modifies another', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Row 1' } }),
          createRow('row2', 2, { col1: { value: 'Row 2' } }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Row 1 Local' } }),
          createRow('row2', 2, { col1: { value: 'Row 2' } }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Row 1' } }),
          createRow('row2', 2, { col1: { value: 'Row 2 Remote' } }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.conflicts).toHaveLength(0);
      expect(result.merged.rows[0].cells.col1.value).toBe('Row 1 Local');
      expect(result.merged.rows[1].cells.col1.value).toBe('Row 2 Remote');
    });
  });

  describe('2. Conflict detection (both modify same field)', () => {
    it('should detect conflict when both modify same cell', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Original' } }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Local Value' } }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Remote Value' } }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(false);
      expect(result.conflicts).toHaveLength(1);
      expect(result.conflicts[0].rowId).toBe('row1');
      expect(result.conflicts[0].columnId).toBe('col1');
      expect(result.conflicts[0].localValue).toBe('Local Value');
      expect(result.conflicts[0].remoteValue).toBe('Remote Value');
      expect(result.conflicts[0].baseValue).toBe('Original');
    });

    it('should detect multiple conflicts in different cells', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Title' }, col2: { value: 'medium' } }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Local Title' }, col2: { value: 'high' } }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, { col1: { value: 'Remote Title' }, col2: { value: 'low' } }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(false);
      expect(result.conflicts).toHaveLength(2);
    });
  });

  describe('3. Add new row (one side adds)', () => {
    it('should include row added by local', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2, { col1: { value: 'New Local Row' } })],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(2);
      expect(result.merged.rows[1].cells.col1.value).toBe('New Local Row');
    });

    it('should include row added by remote', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2, { col1: { value: 'New Remote Row' } })],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(2);
      expect(result.merged.rows[1].cells.col1.value).toBe('New Remote Row');
    });
  });

  describe('4. Delete row (one side deletes)', () => {
    it('should delete row when local deletes', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2)],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2)],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(1);
      expect(result.merged.rows[0].id).toBe('row1');
    });

    it('should delete row when remote deletes', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2)],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2)],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(1);
      expect(result.merged.rows[0].id).toBe('row1');
    });
  });

  describe('5. Both add different new rows', () => {
    it('should include both new rows from local and remote', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1),
          createRow('row2', 2, { col1: { value: 'Local New Row' } }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1),
          createRow('row3', 2, { col1: { value: 'Remote New Row' } }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(3);
      
      const rowIds = result.merged.rows.map(r => r.id);
      expect(rowIds).toContain('row1');
      expect(rowIds).toContain('row2');
      expect(rowIds).toContain('row3');
    });
  });

  describe('6. Both delete same row', () => {
    it('should delete row when both delete the same row', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1), createRow('row2', 2)],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1)],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(1);
      expect(result.merged.rows[0].id).toBe('row1');
    });
  });

  describe('7. Cell-level conflict detection', () => {
    it('should detect conflict at specific cell level, not row level', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Title' },
            col2: { value: 'medium' },
            col3: { value: 'draft' },
          }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Local Title' },
            col2: { value: 'medium' },
            col3: { value: 'draft' },
          }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Remote Title' },
            col2: { value: 'high' },
            col3: { value: 'ready' },
          }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      // col1 has conflict because both local and remote modified it
      expect(result.success).toBe(false);
      expect(result.conflicts).toHaveLength(1);
      expect(result.conflicts[0].rowId).toBe('row1');
      expect(result.conflicts[0].columnId).toBe('col1');
      // col2 and col3 should not be in conflicts since only remote changed them
    });

    it('should merge non-conflicting cells within same row', () => {
      const base = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Title' },
            col2: { value: 'medium' },
            col3: { value: 'draft' },
          }),
        ],
      });

      const local = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Local Title' },
            col2: { value: 'medium' },
            col3: { value: 'draft' },
          }),
        ],
      });

      const remote = createTestFile({
        rows: [
          createRow('row1', 1, {
            col1: { value: 'Title' },
            col2: { value: 'high' },
            col3: { value: 'draft' },
          }),
        ],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      // No conflicts - local changed col1, remote changed col2
      expect(result.success).toBe(true);
      expect(result.conflicts).toHaveLength(0);

      // Merged result should have both changes
      expect(result.merged.rows[0].cells.col1.value).toBe('Local Title');
      expect(result.merged.rows[0].cells.col2.value).toBe('high');
    });
  });

  describe('Edge cases', () => {
    it('should handle empty files', () => {
      const base = createTestFile({ rows: [] });
      const local = createTestFile({ rows: [] });
      const remote = createTestFile({ rows: [] });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(true);
      expect(result.merged.rows).toHaveLength(0);
    });

    it('should handle row deleted locally but modified remotely (conflict)', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1, { col1: { value: 'Original' } })],
      });

      const local = createTestFile({
        rows: [],
      });

      const remote = createTestFile({
        rows: [createRow('row1', 1, { col1: { value: 'Modified Remote' } })],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(false);
      expect(result.conflicts).toHaveLength(1);
      expect(result.conflicts[0].rowId).toBe('row1');
    });

    it('should handle row deleted remotely but modified locally (conflict)', () => {
      const base = createTestFile({
        rows: [createRow('row1', 1, { col1: { value: 'Original' } })],
      });

      const local = createTestFile({
        rows: [createRow('row1', 1, { col1: { value: 'Modified Local' } })],
      });

      const remote = createTestFile({
        rows: [],
      });

      const result = mergeTestCaseFiles(base, local, remote);

      expect(result.success).toBe(false);
      expect(result.conflicts).toHaveLength(1);
      expect(result.conflicts[0].rowId).toBe('row1');
    });
  });
});

describe('resolveConflict', () => {
  it('should resolve conflict with local value', () => {
    const conflict: MergeConflict = {
      rowId: 'row1',
      columnId: 'col1',
      baseValue: 'base',
      localValue: 'local',
      remoteValue: 'remote',
    };

    const resolved = resolveConflict(conflict, 'local');

    expect(resolved.resolved).toBe(true);
    expect(resolved.resolvedValue).toBe('local');
  });

  it('should resolve conflict with remote value', () => {
    const conflict: MergeConflict = {
      rowId: 'row1',
      columnId: 'col1',
      baseValue: 'base',
      localValue: 'local',
      remoteValue: 'remote',
    };

    const resolved = resolveConflict(conflict, 'remote');

    expect(resolved.resolved).toBe(true);
    expect(resolved.resolvedValue).toBe('remote');
  });

  it('should resolve conflict with custom value', () => {
    const conflict: MergeConflict = {
      rowId: 'row1',
      columnId: 'col1',
      baseValue: 'base',
      localValue: 'local',
      remoteValue: 'remote',
    };

    const resolved = resolveConflict(conflict, 'custom', 'custom-value');

    expect(resolved.resolved).toBe(true);
    expect(resolved.resolvedValue).toBe('custom-value');
  });
});

describe('applyResolvedConflicts', () => {
  it('should apply resolved cell-level conflicts', () => {
    const merged = createTestFile({
      rows: [
        createRow('row1', 1, {
          col1: { value: 'Local Value' },
          col2: { value: 'Non-conflicting' },
        }),
      ],
    });

    const conflicts: MergeConflict[] = [
      {
        rowId: 'row1',
        columnId: 'col1',
        baseValue: 'Original',
        localValue: 'Local Value',
        remoteValue: 'Remote Value',
        resolved: true,
        resolvedValue: 'Resolved Value',
      },
    ];

    const result = applyResolvedConflicts(merged, conflicts);

    expect(result.rows[0].cells.col1.value).toBe('Resolved Value');
    expect(result.rows[0].cells.col2.value).toBe('Non-conflicting');
  });

  it('should delete row when resolved value is null', () => {
    const merged = createTestFile({
      rows: [
        createRow('row1', 1),
        createRow('row2', 2),
      ],
    });

    const conflicts: MergeConflict[] = [
      {
        rowId: 'row1',
        baseValue: { id: 'row1' },
        localValue: { id: 'row1' },
        remoteValue: null,
        resolved: true,
        resolvedValue: null,
      },
    ];

    const result = applyResolvedConflicts(merged, conflicts);

    expect(result.rows).toHaveLength(1);
    expect(result.rows[0].id).toBe('row2');
  });

  it('should ignore unresolved conflicts', () => {
    const merged = createTestFile({
      rows: [
        createRow('row1', 1, { col1: { value: 'Local Value' } }),
      ],
    });

    const conflicts: MergeConflict[] = [
      {
        rowId: 'row1',
        columnId: 'col1',
        baseValue: 'Original',
        localValue: 'Local Value',
        remoteValue: 'Remote Value',
        resolved: false,
      },
    ];

    const result = applyResolvedConflicts(merged, conflicts);

    expect(result.rows[0].cells.col1.value).toBe('Local Value');
  });
});
