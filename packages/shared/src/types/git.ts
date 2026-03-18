/**
 * TestCase Manager - Git Integration Types
 * 
 * This module defines all types related to Git version control integration,
 * including credentials, commits, branches, conflicts, and operations.
 */

/**
 * HTTPS authentication credentials
 */
export interface HttpsCredentials {
  /** Authentication type */
  type: 'https';
  /** Username or email */
  username: string;
  /** Password or personal access token */
  password: string;
  /** Whether to store credentials */
  storeCredentials?: boolean;
}

/**
 * SSH authentication credentials
 */
export interface SshCredentials {
  /** Authentication type */
  type: 'ssh';
  /** Path to SSH private key */
  privateKeyPath: string;
  /** SSH key passphrase (optional) */
  passphrase?: string;
  /** SSH agent socket path (optional) */
  agentSocket?: string;
}

/**
 * Git authentication credentials (HTTPS or SSH)
 */
export type GitCredentials = HttpsCredentials | SshCredentials;

/**
 * Git commit author information
 */
export interface GitAuthor {
  /** Author name */
  name: string;
  /** Author email */
  email: string;
  /** Commit timestamp */
  timestamp: string;
}

/**
 * Git commit information
 */
export interface GitCommitInfo {
  /** Commit SHA */
  sha: string;
  /** Short commit SHA (7 characters) */
  shortSha: string;
  /** Commit message */
  message: string;
  /** Commit message summary (first line) */
  summary: string;
  /** Commit message body */
  body?: string;
  /** Author information */
  author: GitAuthor;
  /** Committer information (may differ from author) */
  committer: GitAuthor;
  /** Parent commit SHAs */
  parents: string[];
  /** Commit timestamp */
  timestamp: string;
}

/**
 * Git branch information
 */
export interface GitBranch {
  /** Branch name */
  name: string;
  /** Full reference path (e.g., refs/heads/main) */
  ref: string;
  /** Current commit SHA on this branch */
  commitSha: string;
  /** Whether this is the currently checked out branch */
  isCurrent: boolean;
  /** Whether this is a remote tracking branch */
  isRemote: boolean;
  /** Remote name (for remote branches) */
  remoteName?: string;
  /** Upstream branch name (for local branches) */
  upstream?: string;
  /** Last commit message on this branch */
  lastCommitMessage?: string;
  /** Last commit timestamp */
  lastCommitTimestamp?: string;
}

/**
 * File status in a conflict
 */
export type ConflictFileStatus = 
  | 'both-modified' 
  | 'both-added' 
  | 'deleted-by-us' 
  | 'deleted-by-them' 
  | 'added-by-us' 
  | 'added-by-them';

/**
 * Git conflict information
 */
export interface GitConflict {
  /** File path with conflict */
  filePath: string;
  /** Type of conflict */
  status: ConflictFileStatus;
  /** Base/ancestor version SHA (if available) */
  baseSha?: string;
  /** Our/local version SHA */
  oursSha?: string;
  /** Their/remote version SHA */
  theirsSha?: string;
  /** Conflict markers content */
  conflictContent?: string;
  /** Whether the conflict has been resolved */
  resolved: boolean;
}

/**
 * File status in working directory
 */
export type WorkingFileStatus =
  | 'unmodified'
  | 'modified'
  | 'added'
  | 'deleted'
  | 'renamed'
  | 'copied'
  | 'untracked'
  | 'ignored'
  | 'conflicted';

/**
 * Staged status of a file
 */
export type StagedStatus =
  | 'unmodified'
  | 'modified'
  | 'added'
  | 'deleted'
  | 'renamed'
  | 'copied';

/**
 * File status entry
 */
export interface FileStatus {
  /** File path */
  path: string;
  /** Original path (for renamed files) */
  originalPath?: string;
  /** Working directory status */
  workingStatus: WorkingFileStatus;
  /** Staging area status */
  stagedStatus: StagedStatus;
}

/**
 * Complete Git repository status
 */
export interface GitStatus {
  /** Current branch name */
  currentBranch: string;
  /** Current commit SHA */
  currentCommitSha: string;
  /** Whether there are uncommitted changes */
  isClean: boolean;
  /** Number of untracked files */
  untrackedCount: number;
  /** Number of modified files (not staged) */
  modifiedCount: number;
  /** Number of staged files */
  stagedCount: number;
  /** Number of conflicted files */
  conflictedCount: number;
  /** Detailed file statuses */
  files: FileStatus[];
  /** Current conflicts (if any) */
  conflicts: GitConflict[];
  /** Ahead/behind information for current branch */
  tracking?: {
    /** Remote name */
    remote: string;
    /** Remote branch name */
    remoteBranch: string;
    /** Number of commits ahead of remote */
    ahead: number;
    /** Number of commits behind remote */
    behind: number;
  };
}

/**
 * Pull operation result
 */
export interface PullResult {
  /** Whether the pull was successful */
  success: boolean;
  /** Error message if failed */
  error?: string;
  /** New commits fetched */
  fetchedCommits: GitCommitInfo[];
  /** Commits merged/rebased */
  mergedCommits: GitCommitInfo[];
  /** Files updated */
  updatedFiles: string[];
  /** Conflicts encountered */
  conflicts: GitConflict[];
  /** Whether fast-forward was used */
  fastForward: boolean;
  /** Previous HEAD SHA */
  previousSha?: string;
  /** New HEAD SHA */
  newSha?: string;
}

/**
 * Push operation result
 */
export interface PushResult {
  /** Whether the push was successful */
  success: boolean;
  /** Error message if failed */
  error?: string;
  /** Remote URL */
  remoteUrl: string;
  /** Branch pushed */
  branch: string;
  /** New remote HEAD SHA */
  remoteSha?: string;
  /** Pushed commits */
  pushedCommits: GitCommitInfo[];
}

/**
 * Clone operation result
 */
export interface CloneResult {
  /** Whether the clone was successful */
  success: boolean;
  /** Error message if failed */
  error?: string;
  /** Local path where repository was cloned */
  localPath: string;
  /** Default branch name */
  defaultBranch: string;
  /** Initial commit SHA */
  initialCommitSha: string;
}

/**
 * Merge operation result
 */
export interface MergeResult {
  /** Whether the merge was successful */
  success: boolean;
  /** Error message if failed */
  error?: string;
  /** Source branch/commit */
  source: string;
  /** Target branch */
  target: string;
  /** Merge commit SHA (if created) */
  mergeCommitSha?: string;
  /** Whether merge was a fast-forward */
  fastForward: boolean;
  /** Conflicts encountered */
  conflicts: GitConflict[];
}
