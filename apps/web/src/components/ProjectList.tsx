import type { Project } from '../api/types'
import './App.css'

interface ProjectListProps {
  projects: Project[]
  currentProject: Project | null
  onSelect: (project: Project) => void
}

export function ProjectList({ projects, currentProject, onSelect }: ProjectListProps) {
  return (
    <div className="project-list">
      <div className="project-list-header">项目列表</div>
      {projects.length === 0 ? (
        <div className="empty-state">暂无项目</div>
      ) : (
        projects.map((project) => (
          <div
            key={project.id}
            className={`project-item ${currentProject?.id === project.id ? 'active' : ''}`}
            onClick={() => onSelect(project)}
          >
            <span className="project-item-icon"></span>
            {project.name}
          </div>
        ))
      )}
    </div>
  )
}
