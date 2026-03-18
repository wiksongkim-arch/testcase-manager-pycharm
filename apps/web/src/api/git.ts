import { apiClient } from './client'
import type { GitStatus } from './types'

export const gitApi = {
  // 获取 Git 状态
  async getStatus(projectId: string): Promise<GitStatus> {
    return apiClient.get(`/projects/${projectId}/git/status`)
  },

  // 拉取代码
  async pull(projectId: string): Promise<{ success: boolean; conflicts?: any[] }> {
    return apiClient.post(`/projects/${projectId}/git/pull`)
  },

  // 推送代码
  async push(projectId: string): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/push`)
  },

  // 提交更改
  async commit(projectId: string, message: string): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/commit`, { message })
  },

  // 切换分支
  async checkout(projectId: string, branch: string): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/checkout`, { branch })
  },

  // 获取分支列表
  async getBranches(projectId: string): Promise<string[]> {
    return apiClient.get(`/projects/${projectId}/git/branches`)
  },

  // 解决冲突
  async resolveConflicts(projectId: string, resolved: any[]): Promise<void> {
    return apiClient.post(`/projects/${projectId}/git/resolve`, { resolved })
  },

  // 获取提交历史
  async getLog(projectId: string, limit?: number): Promise<any[]> {
    return apiClient.get(`/projects/${projectId}/git/log`, {
      params: { limit },
    })
  },
}
