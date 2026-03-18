import * as vscode from 'vscode';
import { TestCasePanel } from './webview/panel';
import { ProjectsProvider } from './treeView/projectsProvider';
import { apiClient } from './api/client';

export class Commands {
  private context: vscode.ExtensionContext;

  constructor(context: vscode.ExtensionContext) {
    this.context = context;
  }

  register() {
    this.context.subscriptions.push(
      vscode.commands.registerCommand('testcase-manager.open', () => {
        TestCasePanel.createOrShow(this.context.extensionUri);
      }),

      vscode.commands.registerCommand('testcase-manager.refresh', () => {
        vscode.commands.executeCommand('testcaseManagerProjects.refresh');
      }),

      vscode.commands.registerCommand('testcase-manager.clone', async () => {
        const repoUrl = await vscode.window.showInputBox({
          prompt: '输入 Git 仓库地址',
          placeHolder: 'https://github.com/username/repo.git',
        });

        if (!repoUrl) return;

        const projectName = await vscode.window.showInputBox({
          prompt: '输入项目名称',
          placeHolder: 'my-project',
        });

        if (!projectName) return;

        try {
          await vscode.window.withProgress(
            {
              location: vscode.ProgressLocation.Notification,
              title: '正在克隆仓库...',
              cancellable: false,
            },
            async () => {
              await apiClient.clone(repoUrl, projectName);
            }
          );
          vscode.window.showInformationMessage(`项目 ${projectName} 克隆成功`);
          vscode.commands.executeCommand('testcaseManagerProjects.refresh');
        } catch (error: any) {
          vscode.window.showErrorMessage(`克隆失败: ${error.message}`);
        }
      }),

      vscode.commands.registerCommand('testcase-manager.openProject', async (projectId: string) => {
        TestCasePanel.createOrShow(this.context.extensionUri, projectId);
      }),

      vscode.commands.registerCommand('testcase-manager.commit', async (projectId: string) => {
        const message = await vscode.window.showInputBox({
          prompt: '输入提交信息',
          placeHolder: '更新测试用例',
        });

        if (!message) return;

        try {
          await apiClient.commit(projectId, message);
          vscode.window.showInformationMessage('提交成功');
        } catch (error: any) {
          vscode.window.showErrorMessage(`提交失败: ${error.message}`);
        }
      }),

      vscode.commands.registerCommand('testcase-manager.push', async (projectId: string) => {
        try {
          await vscode.window.withProgress(
            {
              location: vscode.ProgressLocation.Notification,
              title: '正在推送...',
              cancellable: false,
            },
            async () => {
              await apiClient.push(projectId);
            }
          );
          vscode.window.showInformationMessage('推送成功');
        } catch (error: any) {
          vscode.window.showErrorMessage(`推送失败: ${error.message}`);
        }
      }),

      vscode.commands.registerCommand('testcase-manager.pull', async (projectId: string) => {
        try {
          const result = await vscode.window.withProgress(
            {
              location: vscode.ProgressLocation.Notification,
              title: '正在拉取...',
              cancellable: false,
            },
            async () => {
              return await apiClient.pull(projectId);
            }
          );

          if (result.conflicts && result.conflicts.length > 0) {
            vscode.window.showWarningMessage(
              `拉取完成，但有 ${result.conflicts.length} 个冲突需要解决`
            );
          } else {
            vscode.window.showInformationMessage('拉取成功');
          }
        } catch (error: any) {
          vscode.window.showErrorMessage(`拉取失败: ${error.message}`);
        }
      })
    );
  }
}
