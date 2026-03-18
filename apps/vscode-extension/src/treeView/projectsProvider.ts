import * as vscode from 'vscode';
import { apiClient, Project } from '../api/client';
import { ProjectItem } from './projectItem';

export class ProjectsProvider implements vscode.TreeDataProvider<ProjectItem> {
  private _onDidChangeTreeData: vscode.EventEmitter<ProjectItem | undefined | null | void> =
    new vscode.EventEmitter<ProjectItem | undefined | null | void>();
  readonly onDidChangeTreeData: vscode.Event<ProjectItem | undefined | null | void> =
    this._onDidChangeTreeData.event;

  private projects: Project[] = [];

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this._onDidChangeTreeData.fire();
  }

  getTreeItem(element: ProjectItem): vscode.TreeItem {
    return element;
  }

  async getChildren(element?: ProjectItem): Promise<ProjectItem[]> {
    if (element) {
      return [];
    }

    try {
      this.projects = await apiClient.listProjects();
      return this.projects.map(
        (project) =>
          new ProjectItem(
            project.id,
            project.name,
            project.description,
            project.path,
            vscode.TreeItemCollapsibleState.None
          )
      );
    } catch (error: any) {
      vscode.window.showErrorMessage(`加载项目列表失败: ${error.message}`);
      return [
        new ProjectItem(
          'error',
          '无法加载项目',
          '请检查 API 服务是否运行',
          '',
          vscode.TreeItemCollapsibleState.None,
          {
            command: '',
            title: '',
            arguments: [],
          }
        ),
      ];
    }
  }

  async getProjectById(id: string): Promise<Project | undefined> {
    try {
      return await apiClient.getProject(id);
    } catch (error) {
      return undefined;
    }
  }
}
