import { apiClient } from './client'
import type { Project, TestCase } from './types'

export const projectsApi = {
  // 获取所有项目
  async list(): Promise<Project[]> {
    return apiClient.get('/projects')
  },

  // 获取单个项目
  async get(id: string): Promise<Project> {
    return apiClient.get(`/projects/${id}`)
  },

  // 创建项目
  async create(data: Omit<Project, 'id' | 'createdAt' | 'updatedAt'>): Promise<Project> {
    return apiClient.post('/projects', data)
  },

  // 更新项目
  async update(id: string, data: Partial<Project>): Promise<Project> {
    return apiClient.put(`/projects/${id}`, data)
  },

  // 删除项目
  async delete(id: string): Promise<void> {
    return apiClient.delete(`/projects/${id}`)
  },

  // 获取项目的测试用例
  async getTestCases(projectId: string, suite?: string): Promise<TestCase[]> {
    const params = suite ? { suite } : {}
    return apiClient.get(`/projects/${projectId}/testcases`, { params })
  },

  // 保存测试用例
  async saveTestCases(projectId: string, testCases: TestCase[]): Promise<void> {
    return apiClient.post(`/projects/${projectId}/testcases`, { testCases })
  },

  // Git 操作
  async pull(projectId: string): Promise<{ success: boolean; conflicts?: any[] }> {
    return apiClient.post(`/projects/${projectId}/git/pull`)
  },

  async push(projectId: string): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/push`)
  },

  async commit(projectId: string, message: string): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/commit`, { message })
  },

  async resolveConflicts(projectId: string, resolved: any[]): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/resolve`, { resolved })
  },
}
