import * as vscode from 'vscode';
import * as path from 'path';

export class ProjectItem extends vscode.TreeItem {
  constructor(
    public readonly id: string,
    public readonly label: string,
    public readonly description: string | undefined,
    public readonly projectPath: string,
    public readonly collapsibleState: vscode.TreeItemCollapsibleState,
    public readonly command?: vscode.Command
  ) {
    super(label, collapsibleState);

    this.tooltip = `${this.label}${description ? ` - ${description}` : ''}`;
    this.description = description;
    this.contextValue = 'project';

    // 设置图标
    this.iconPath = new vscode.ThemeIcon('folder');

    // 设置点击命令
    if (!command) {
      this.command = {
        command: 'testcase-manager.openProject',
        title: '打开项目',
        arguments: [this.id],
      };
    }
  }
}
