import { apiClient } from './client'
import type { TestCase } from './types'

export const testCasesApi = {
  // 获取单个测试用例
  async get(id: string): Promise<TestCase> {
    return apiClient.get(`/testcases/${id}`)
  },

  // 创建测试用例
  async create(data: Omit<TestCase, 'id'>): Promise<TestCase> {
    return apiClient.post('/testcases', data)
  },

  // 更新测试用例
  async update(id: string, data: Partial<TestCase>): Promise<TestCase> {
    return apiClient.put(`/testcases/${id}`, data)
  },

  // 删除测试用例
  async delete(id: string): Promise<void> {
    return apiClient.delete(`/testcases/${id}`)
  },

  // 批量更新测试用例
  async batchUpdate(testCases: TestCase[]): Promise<TestCase[]> {
    return apiClient.post('/testcases/batch', { testCases })
  },

  // 批量删除测试用例
  async batchDelete(ids: string[]): Promise<void> {
    return apiClient.post('/testcases/batch-delete', { ids })
  },
}
