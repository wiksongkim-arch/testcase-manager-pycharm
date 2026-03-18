// Git Core - Main exports
export { GitService } from './git-service';

export {
  mergeTestCaseFiles,
  resolveConflict,
  applyResolvedConflicts,
} from './merge';
export type {
  MergeResult,
  MergeConflict,
} from './merge';

// Re-export types from @testcase-manager/shared for convenience
export type {
  GitCredentials,
  GitAuthor,
  PullResult,
  GitStatus,
  FileStatus,
  GitCommitInfo,
  GitBranch,
  GitConflict,
  PushResult,
  CloneResult,
  MergeResult as GitMergeResult,
  TestCaseFile,
  RowData,
  CellData,
  ColumnDefinition,
} from '@testcase-manager/shared';
