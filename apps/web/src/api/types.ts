export interface Project {
  id: string
  name: string
  path: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface TestCase {
  id: string
  title: string
  precondition: string
  steps: string
  expectedResult: string
  priority: 'P0' | 'P1' | 'P2' | 'P3'
  status: '草稿' | '评审中' | '已发布' | '已废弃'
  tags: string[]
  suite?: string
}

export interface GitStatus {
  branch: string
  ahead: number
  behind: number
  modified: string[]
  staged: string[]
  conflicts: string[]
}

export interface PullResult {
  success: boolean
  conflicts?: Array<{
    id: string
    field: string
    local: string
    remote: string
  }>
}
