import * as vscode from 'vscode';
import { Commands } from './commands';
import { ProjectsProvider } from './treeView/projectsProvider';

export function activate(context: vscode.ExtensionContext) {
  console.log('TestCase Manager extension is now active');

  // 设置上下文，启用视图
  vscode.commands.executeCommand('setContext', 'testcase-manager.enabled', true);

  // 初始化命令
  const commands = new Commands(context);
  commands.register();

  // 初始化树视图
  const projectsProvider = new ProjectsProvider();
  const treeView = vscode.window.createTreeView('testcaseManagerProjects', {
    treeDataProvider: projectsProvider,
    showCollapseAll: true,
  });

  context.subscriptions.push(treeView);

  // 监听配置变化
  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration((e) => {
      if (e.affectsConfiguration('testcaseManager')) {
        projectsProvider.refresh();
      }
    })
  );
}

export function deactivate() {
  console.log('TestCase Manager extension is now deactivated');
}
