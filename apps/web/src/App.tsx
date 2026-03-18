import { useState, useEffect } from 'react'
import { Layout } from './components/Layout'
import { ProjectList } from './components/ProjectList'
import { TestCaseTable } from './components/TestCaseTable'
import { GitToolbar } from './components/GitToolbar'
import { ConflictResolver } from './components/ConflictResolver'
import { AppProvider, useAppState } from './store'
import { projectsApi } from './api/projects'
import type { Project, TestCase } from './api/types'
import './App.css'

function AppContent() {
  const { state, dispatch } = useAppState()
  const [projects, setProjects] = useState<Project[]>([])
  const [testCases, setTestCases] = useState<TestCase[]>([])
  const [loading, setLoading] = useState(false)
  const [showConflictResolver, setShowConflictResolver] = useState(false)
  const [conflicts, setConflicts] = useState<any[]>([])

  // 加载项目列表
  useEffect(() => {
    loadProjects()
  }, [])

  // 加载当前项目的测试用例
  useEffect(() => {
    if (state.currentProject) {
      loadTestCases(state.currentProject.id)
    }
  }, [state.currentProject, state.currentSuite])

  const loadProjects = async () => {
    try {
      const data = await projectsApi.list()
      setProjects(data)
      if (data.length > 0 && !state.currentProject) {
        dispatch({ type: 'SET_PROJECT', payload: data[0] })
      }
    } catch (error) {
      console.error('Failed to load projects:', error)
    }
  }

  const loadTestCases = async (projectId: string) => {
    setLoading(true)
    try {
      const data = await projectsApi.getTestCases(projectId, state.currentSuite || undefined)
      setTestCases(data)
    } catch (error) {
      console.error('Failed to load test cases:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleProjectSelect = (project: Project) => {
    dispatch({ type: 'SET_PROJECT', payload: project })
  }

  const handleTestCasesChange = (newTestCases: TestCase[]) => {
    setTestCases(newTestCases)
  }

  const handleSaveTestCases = async () => {
    if (!state.currentProject) return
    
    try {
      await projectsApi.saveTestCases(state.currentProject.id, testCases)
      dispatch({ type: 'SET_DIRTY', payload: false })
    } catch (error) {
      console.error('Failed to save test cases:', error)
      alert('保存失败: ' + (error as Error).message)
    }
  }

  const handlePull = async () => {
    if (!state.currentProject) return
    
    try {
      const result = await projectsApi.pull(state.currentProject.id)
      if (result.conflicts && result.conflicts.length > 0) {
        setConflicts(result.conflicts)
        setShowConflictResolver(true)
      } else {
        await loadTestCases(state.currentProject.id)
        alert('拉取成功')
      }
    } catch (error) {
      console.error('Failed to pull:', error)
      alert('拉取失败: ' + (error as Error).message)
    }
  }

  const handlePush = async () => {
    if (!state.currentProject) return
    
    try {
      await projectsApi.push(state.currentProject.id)
      alert('推送成功')
    } catch (error) {
      console.error('Failed to push:', error)
      alert('推送失败: ' + (error as Error).message)
    }
  }

  const handleCommit = async (message: string) => {
    if (!state.currentProject) return
    
    try {
      await projectsApi.commit(state.currentProject.id, message)
      alert('提交成功')
    } catch (error) {
      console.error('Failed to commit:', error)
      alert('提交失败: ' + (error as Error).message)
    }
  }

  const handleResolveConflicts = async (resolved: any[]) => {
    if (!state.currentProject) return
    
    try {
      await projectsApi.resolveConflicts(state.currentProject.id, resolved)
      setShowConflictResolver(false)
      await loadTestCases(state.currentProject.id)
      alert('冲突已解决')
    } catch (error) {
      console.error('Failed to resolve conflicts:', error)
      alert('解决冲突失败: ' + (error as Error).message)
    }
  }

  return (
    <div className="app">
      <GitToolbar
        currentProject={state.currentProject}
        currentBranch={state.currentBranch}
        onPull={handlePull}
        onPush={handlePush}
        onCommit={handleCommit}
        onBranchChange={(branch) => dispatch({ type: 'SET_BRANCH', payload: branch })}
      />
      <Layout
        sidebar={
          <ProjectList
            projects={projects}
            currentProject={state.currentProject}
            onSelect={handleProjectSelect}
          />
        }
      >
        {state.currentProject ? (
          <TestCaseTable
            testCases={testCases}
            loading={loading}
            currentSuite={state.currentSuite}
            onChange={handleTestCasesChange}
            onSave={handleSaveTestCases}
            onSuiteChange={(suite) => dispatch({ type: 'SET_SUITE', payload: suite })}
          />
        ) : (
          <div className="empty-state">
            <p>请选择一个项目或创建新项目</p>
          </div>
        )}
      </Layout>

      {showConflictResolver && (
        <ConflictResolver
          conflicts={conflicts}
          onResolve={handleResolveConflicts}
          onCancel={() => setShowConflictResolver(false)}
        />
      )}
    </div>
  )
}

function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  )
}

export default App
