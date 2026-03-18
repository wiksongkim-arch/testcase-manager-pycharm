import * as git from 'isomorphic-git';
import http from 'isomorphic-git/http/node';
import * as fs from 'fs-extra';
import * as path from 'path';
import type {
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
  MergeResult,
} from '@testcase-manager/shared';

export class GitService {
  /**
   * Clone a repository
   */
  async clone(
    repoUrl: string,
    localPath: string,
    credentials?: GitCredentials
  ): Promise<CloneResult> {
    await fs.ensureDir(localPath);
    
    const options: any = {
      fs,
      http,
      dir: localPath,
      url: repoUrl,
      singleBranch: false,
      depth: 1,
    };

    if (credentials) {
      if (credentials.type === 'https') {
        options.onAuth = () => ({
          username: credentials.username,
          password: credentials.password,
        });
      }
    }

    await git.clone(options);

    const defaultBranch = await this.getCurrentBranch(localPath);
    const initialCommitSha = await git.resolveRef({ fs, dir: localPath, ref: 'HEAD' });

    return {
      success: true,
      localPath,
      defaultBranch,
      initialCommitSha,
    };
  }

  /**
   * Initialize a new repository
   */
  async init(localPath: string): Promise<void> {
    await fs.ensureDir(localPath);
    await git.init({ fs, dir: localPath, defaultBranch: 'main' });
  }

  /**
   * Add a file to staging
   */
  async add(localPath: string, filepath: string): Promise<void> {
    await git.add({ fs, dir: localPath, filepath });
  }

  /**
   * Add all files to staging
   */
  async addAll(localPath: string): Promise<void> {
    const status = await git.statusMatrix({ fs, dir: localPath });
    
    for (const [filepath, headStatus, workdirStatus, stageStatus] of status) {
      // If file is modified or untracked, add it
      if (workdirStatus !== 1 || stageStatus !== 1) {
        await git.add({ fs, dir: localPath, filepath });
      }
    }
  }

  /**
   * Commit changes
   */
  async commit(
    localPath: string,
    message: string,
    author: GitAuthor
  ): Promise<string> {
    const oid = await git.commit({
      fs,
      dir: localPath,
      message,
      author: {
        name: author.name,
        email: author.email,
      },
    });
    return oid;
  }

  /**
   * Push to remote
   */
  async push(
    localPath: string,
    remote: string,
    branch: string,
    credentials?: GitCredentials
  ): Promise<PushResult> {
    const options: any = {
      fs,
      http,
      dir: localPath,
      remote,
      ref: branch,
    };

    if (credentials) {
      if (credentials.type === 'https') {
        options.onAuth = () => ({
          username: credentials.username,
          password: credentials.password,
        });
      }
    }

    await git.push(options);

    return {
      success: true,
      remoteUrl: remote,
      branch,
      pushedCommits: [], // Would need to track which commits were pushed
    };
  }

  /**
   * Pull from remote
   */
  async pull(
    localPath: string,
    remote: string,
    branch: string,
    credentials?: GitCredentials
  ): Promise<PullResult> {
    try {
      const previousSha = await git.resolveRef({ fs, dir: localPath, ref: 'HEAD' });

      const options: any = {
        fs,
        http,
        dir: localPath,
        remote,
        ref: branch,
        fastForwardOnly: false,
      };

      if (credentials) {
        if (credentials.type === 'https') {
          options.onAuth = () => ({
            username: credentials.username,
            password: credentials.password,
          });
        }
      }

      await git.pull(options);
      const newSha = await git.resolveRef({ fs, dir: localPath, ref: 'HEAD' });

      // Check for conflicts after pull
      const conflicts = await this.getConflicts(localPath);

      return {
        success: true,
        fetchedCommits: [], // Would need to track fetched commits
        mergedCommits: [], // Would need to track merged commits
        updatedFiles: [], // Would need to track updated files
        conflicts,
        fastForward: true, // Simplified - actual detection needed
        previousSha,
        newSha,
      };
    } catch (error: any) {
      // Check if it's a merge conflict error
      if (error.message && error.message.includes('merge')) {
        const conflicts = await this.getConflicts(localPath);
        return {
          success: false,
          fetchedCommits: [],
          mergedCommits: [],
          updatedFiles: [],
          conflicts,
          fastForward: false,
          error: error.message,
        };
      }
      throw error;
    }
  }

  /**
   * Get list of branches
   */
  async getBranches(localPath: string): Promise<GitBranch[]> {
    const branches = await git.listBranches({ fs, dir: localPath });
    const currentBranch = await this.getCurrentBranch(localPath);

    return Promise.all(
      branches.map(async (name) => {
        const ref = `refs/heads/${name}`;
        let commitSha: string;
        try {
          commitSha = await git.resolveRef({ fs, dir: localPath, ref });
        } catch {
          commitSha = '';
        }

        return {
          name,
          ref,
          commitSha,
          isCurrent: name === currentBranch,
          isRemote: false,
        };
      })
    );
  }

  /**
   * Create a new branch
   */
  async createBranch(localPath: string, branchName: string): Promise<void> {
    await git.branch({ fs, dir: localPath, ref: branchName, checkout: false });
  }

  /**
   * Checkout a branch or commit
   */
  async checkout(localPath: string, ref: string): Promise<void> {
    await git.checkout({ fs, dir: localPath, ref });
  }

  /**
   * Get repository status
   */
  async getStatus(localPath: string): Promise<GitStatus> {
    const status = await git.statusMatrix({ fs, dir: localPath });
    const currentBranch = await this.getCurrentBranch(localPath);
    let currentCommitSha: string;
    try {
      currentCommitSha = await git.resolveRef({ fs, dir: localPath, ref: 'HEAD' });
    } catch {
      currentCommitSha = '';
    }

    const files: FileStatus[] = [];
    let untrackedCount = 0;
    let modifiedCount = 0;
    let stagedCount = 0;
    let conflictedCount = 0;

    for (const [filepath, headStatus, workdirStatus, stageStatus] of status) {
      // headStatus: 0=absent, 1=unchanged, 2=modified, 3=deleted
      // workdirStatus: 0=absent, 1=unchanged, 2=modified, 3=deleted
      // stageStatus: 0=absent, 1=unchanged, 2=modified, 3=deleted

      let workingStatus: FileStatus['workingStatus'] = 'unmodified';
      let stagedStatus: FileStatus['stagedStatus'] = 'unmodified';

      // Untracked file: not in HEAD, modified in workdir, not staged
      if (headStatus === 0 && workdirStatus === 2 && stageStatus === 0) {
        workingStatus = 'untracked';
        untrackedCount++;
      }
      // Staged file: modified in stage
      else if (stageStatus === 2 || stageStatus === 3) {
        stagedStatus = stageStatus === 2 ? 'modified' : 'deleted';
        stagedCount++;
        
        // Also check if workdir has further modifications
        if (workdirStatus === 2) {
          workingStatus = 'modified';
          modifiedCount++;
        }
      }
      // Modified but not staged (and not untracked)
      else if (workdirStatus === 2 && headStatus !== 0) {
        workingStatus = 'modified';
        modifiedCount++;
      }

      files.push({
        path: filepath,
        workingStatus,
        stagedStatus,
      });
    }

    return {
      currentBranch,
      currentCommitSha,
      isClean: untrackedCount === 0 && modifiedCount === 0 && stagedCount === 0 && conflictedCount === 0,
      untrackedCount,
      modifiedCount,
      stagedCount,
      conflictedCount,
      files,
      conflicts: [],
    };
  }

  /**
   * Get conflicted files
   */
  async getConflicts(localPath: string): Promise<GitConflict[]> {
    const status = await git.statusMatrix({ fs, dir: localPath });
    const conflicts: GitConflict[] = [];

    for (const [filepath, headStatus, workdirStatus, stageStatus] of status) {
      // Conflict: both workdir and stage have modifications
      if (workdirStatus === 2 && stageStatus === 2) {
        conflicts.push({
          filePath: filepath,
          status: 'both-modified',
          resolved: false,
        });
      }
    }

    return conflicts;
  }

  /**
   * Get commit history
   */
  async getCommitHistory(
    localPath: string,
    ref: string = 'HEAD',
    depth: number = 10
  ): Promise<GitCommitInfo[]> {
    const commits = await git.log({
      fs,
      dir: localPath,
      ref,
      depth,
    });

    return commits.map((commit: any) => ({
      sha: commit.oid,
      shortSha: commit.oid.slice(0, 7),
      message: commit.commit.message,
      summary: commit.commit.message.split('\n')[0],
      body: commit.commit.message.split('\n').slice(1).join('\n').trim(),
      author: {
        name: commit.commit.author.name,
        email: commit.commit.author.email,
        timestamp: new Date(commit.commit.author.timestamp * 1000).toISOString(),
      },
      committer: {
        name: commit.commit.committer.name,
        email: commit.commit.committer.email,
        timestamp: new Date(commit.commit.committer.timestamp * 1000).toISOString(),
      },
      parents: commit.commit.parent,
      timestamp: new Date(commit.commit.author.timestamp * 1000).toISOString(),
    }));
  }

  /**
   * Check if a directory is a git repository
   */
  async isRepo(localPath: string): Promise<boolean> {
    try {
      await git.findRoot({ fs, filepath: localPath });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get current branch name
   */
  async getCurrentBranch(localPath: string): Promise<string> {
    const branch = await git.currentBranch({ fs, dir: localPath, fullname: false });
    return branch || 'HEAD';
  }
}
