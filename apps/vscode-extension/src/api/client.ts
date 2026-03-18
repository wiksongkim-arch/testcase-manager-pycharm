import axios, { AxiosInstance } from 'axios';
import * as vscode from 'vscode';

// API 响应类型
export interface Project {
  id: string;
  name: string;
  path: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestCase {
  id: string;
  title: string;
  precondition: string;
  steps: string;
  expectedResult: string;
  priority: 'P0' | 'P1' | 'P2' | 'P3';
  status: '草稿' | '评审中' | '已发布' | '已废弃';
  tags: string[];
  suite?: string;
}

export interface GitStatus {
  currentBranch: string;
  currentCommitSha: string;
  isClean: boolean;
  untrackedCount: number;
  modifiedCount: number;
  stagedCount: number;
  conflictedCount: number;
  files: any[];
  conflicts: any[];
  tracking?: {
    remote: string;
    remoteBranch: string;
    ahead: number;
    behind: number;
  };
}

export interface PullResult {
  success: boolean;
  error?: string;
  conflicts?: any[];
}

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // 请求拦截器 - 动态获取配置
    this.client.interceptors.request.use((config) => {
      const vscodeConfig = vscode.workspace.getConfiguration('testcaseManager');
      const apiUrl = vscodeConfig.get<string>('apiUrl') || 'http://localhost:3001';
      config.baseURL = `${apiUrl}/api`;
      return config;
    });

    // 响应拦截器
    this.client.interceptors.response.use(
      (response) => response.data,
      (error) => {
        if (error.response) {
          const message = error.response.data?.message || error.response.statusText;
          return Promise.reject(new Error(message));
        } else if (error.request) {
          return Promise.reject(new Error('无法连接到服务器，请检查后端服务是否运行'));
        } else {
          return Promise.reject(new Error(error.message));
        }
      }
    );
  }

  // 项目相关 API
  async listProjects(): Promise<Project[]> {
    return this.client.get('/projects');
  }

  async getProject(id: string): Promise<Project> {
    return this.client.get(`/projects/${id}`);
  }

  async createProject(data: Omit<Project, 'id' | 'createdAt' | 'updatedAt'>): Promise<Project> {
    return this.client.post('/projects', data);
  }

  async updateProject(id: string, data: Partial<Project>): Promise<Project> {
    return this.client.put(`/projects/${id}`, data);
  }

  async deleteProject(id: string): Promise<void> {
    return this.client.delete(`/projects/${id}`);
  }

  // 测试用例相关 API
  async getTestCases(projectId: string, suite?: string): Promise<TestCase[]> {
    const params = suite ? { suite } : {};
    return this.client.get(`/projects/${projectId}/testcases`, { params });
  }

  async saveTestCases(projectId: string, testCases: TestCase[]): Promise<void> {
    return this.client.post(`/projects/${projectId}/testcases`, { testCases });
  }

  async getTestCase(id: string): Promise<TestCase> {
    return this.client.get(`/testcases/${id}`);
  }

  async createTestCase(data: Omit<TestCase, 'id'>): Promise<TestCase> {
    return this.client.post('/testcases', data);
  }

  async updateTestCase(id: string, data: Partial<TestCase>): Promise<TestCase> {
    return this.client.put(`/testcases/${id}`, data);
  }

  async deleteTestCase(id: string): Promise<void> {
    return this.client.delete(`/testcases/${id}`);
  }

  // Git 相关 API
  async getGitStatus(projectId: string): Promise<GitStatus> {
    return this.client.get(`/projects/${projectId}/git/status`);
  }

  async pull(projectId: string): Promise<PullResult> {
    return this.client.post(`/projects/${projectId}/git/pull`);
  }

  async push(projectId: string): Promise<void> {
    return this.client.post(`/projects/${projectId}/git/push`);
  }

  async commit(projectId: string, message: string): Promise<void> {
    return this.client.post(`/projects/${projectId}/git/commit`, { message });
  }

  async checkout(projectId: string, branch: string): Promise<void> {
    return this.client.post(`/projects/${projectId}/git/checkout`, { branch });
  }

  async getBranches(projectId: string): Promise<string[]> {
    return this.client.get(`/projects/${projectId}/git/branches`);
  }

  async resolveConflicts(projectId: string, resolved: any[]): Promise<void> {
    return this.client.post(`/projects/${projectId}/git/resolve`, { resolved });
  }

  async getLog(projectId: string, limit?: number): Promise<any[]> {
    return this.client.get(`/projects/${projectId}/git/log`, {
      params: { limit },
    });
  }

  // 克隆仓库
  async clone(repoUrl: string, projectName: string): Promise<void> {
    return this.client.post('/clone', { repoUrl, projectName });
  }
}

export const apiClient = new ApiClient();
