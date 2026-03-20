import { useState } from 'react'
import '../App.css'

interface Conflict {
  id: string
  field: string
  local: string
  remote: string
}

interface ConflictResolverProps {
  conflicts: Conflict[]
  onResolve: (resolved: Conflict[]) => void
  onCancel: () => void
}

export function ConflictResolver({ conflicts, onResolve, onCancel }: ConflictResolverProps) {
  const [resolvedConflicts, setResolvedConflicts] = useState<Map<string, string>>(new Map())

  const handleChoose = (conflictId: string, side: 'local' | 'remote') => {
    const conflict = conflicts.find((c) => c.id === conflictId)
    if (!conflict) return

    const newResolved = new Map(resolvedConflicts)
    newResolved.set(conflictId, side === 'local' ? conflict.local : conflict.remote)
    setResolvedConflicts(newResolved)
  }

  const handleResolve = () => {
    const resolved = conflicts.map((conflict) => ({
      ...conflict,
      resolvedValue: resolvedConflicts.get(conflict.id) || conflict.local,
    }))
    onResolve(resolved)
  }

  const allResolved = conflicts.every((c) => resolvedConflicts.has(c.id))

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">解决冲突 ({conflicts.length} 个)</span>
          <button className="modal-close" onClick={onCancel}>×</button>
        </div>

        <div className="modal-body">
          {conflicts.map((conflict) => (
            <div key={conflict.id} className="conflict-item">
              <div className="conflict-header">
                冲突: {conflict.field} (ID: {conflict.id})
              </div>
              <div className="conflict-content">
                <div className="conflict-side">
                  <div className="conflict-side-title">本地版本</div>
                  <div className="conflict-side-content">{conflict.local || '(空)'}</div>
                </div>
                <div className="conflict-side">
                  <div className="conflict-side-title">远程版本</div>
                  <div className="conflict-side-content">{conflict.remote || '(空)'}</div>
                </div>
              </div>
              <div className="conflict-actions">
                <button
                  className={`conflict-button ${resolvedConflicts.get(conflict.id) === conflict.local ? 'primary' : ''}`}
                  onClick={() => handleChoose(conflict.id, 'local')}
                >
                  使用本地
                </button>
                <button
                  className={`conflict-button ${resolvedConflicts.get(conflict.id) === conflict.remote ? 'primary' : ''}`}
                  onClick={() => handleChoose(conflict.id, 'remote')}
                >
                  使用远程
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="modal-footer">
          <button className="git-toolbar-button" onClick={onCancel}>
            取消
          </button>
          <button
            className="git-toolbar-button primary"
            onClick={handleResolve}
            disabled={!allResolved}
          >
            确认解决
          </button>
        </div>
      </div>
    </div>
  )
}
