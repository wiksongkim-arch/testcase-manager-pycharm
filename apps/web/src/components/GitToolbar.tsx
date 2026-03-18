import { useState } from 'react'
import type { Project } from '../api/types'
import './App.css'

interface GitToolbarProps {
  currentProject: Project | null
  currentBranch: string | null
  onPull: () => void
  onPush: () => void
  onCommit: (message: string) => void
  onBranchChange: (branch: string) => void
}

const BRANCHES = ['main', 'develop', 'feature/test-cases', 'release/v1.0']

export function GitToolbar({
  currentProject,
  currentBranch,
  onPull,
  onPush,
  onCommit,
  onBranchChange,
}: GitToolbarProps) {
  const [commitMessage, setCommitMessage] = useState('')
  const [showCommitInput, setShowCommitInput] = useState(false)

  const handleCommit = () => {
    if (!commitMessage.trim()) {
      alert('请输入提交信息')
      return
    }
    onCommit(commitMessage)
    setCommitMessage('')
    setShowCommitInput(false)
  }

  return (
    <div className="git-toolbar">
      <span className="git-toolbar-title">TestCase Manager</span>

      <select
        className="git-toolbar-select"
        value={currentProject?.id || ''}
        disabled={!currentProject}
        onChange={(e) => {
          // 项目切换由 ProjectList 处理
        }}
      >
        <option value={currentProject?.id || ''}>
          {currentProject?.name || '选择项目'}
        </option>
      </select>

      <select
        className="git-toolbar-select"
        value={currentBranch || 'main'}
        onChange={(e) => onBranchChange(e.target.value)}
      >
        {BRANCHES.map((branch) => (
          <option key={branch} value={branch}>{branch}</option>
        ))}
      </select>

      <button className="git-toolbar-button" onClick={onPull}>
        ↓ 拉取
      </button>

      {showCommitInput ? (
        <>
          <input
            type="text"
            placeholder="提交信息..."
            value={commitMessage}
            onChange={(e) => setCommitMessage(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleCommit()
              if (e.key === 'Escape') setShowCommitInput(false)
            }}
            style={{
              padding: '4px 8px',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              fontSize: '13px',
              width: '200px',
            }}
            autoFocus
          />
          <button className="git-toolbar-button primary" onClick={handleCommit}>
            确认
          </button>
          <button className="git-toolbar-button" onClick={() => setShowCommitInput(false)}>
            取消
          </button>
        </>
      ) : (
        <button className="git-toolbar-button primary" onClick={() => setShowCommitInput(true)}>
          ✓ 提交
        </button>
      )}

      <button className="git-toolbar-button" onClick={onPush}>
        ↑ 推送
      </button>
    </div>
  )
}
