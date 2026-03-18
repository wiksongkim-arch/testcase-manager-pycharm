import * as vscode from 'vscode';
import { getWebviewContent } from './content';

export class TestCasePanel {
  public static currentPanel: TestCasePanel | undefined;
  public static readonly viewType = 'testcaseManager';

  private readonly _panel: vscode.WebviewPanel;
  private readonly _extensionUri: vscode.Uri;
  private _projectId: string | undefined;
  private _disposables: vscode.Disposable[] = [];

  public static createOrShow(extensionUri: vscode.Uri, projectId?: string) {
    const column = vscode.window.activeTextEditor
      ? vscode.window.activeTextEditor.viewColumn
      : undefined;

    // 如果面板已存在，显示它
    if (TestCasePanel.currentPanel) {
      TestCasePanel.currentPanel._panel.reveal(column);
      if (projectId) {
        TestCasePanel.currentPanel._projectId = projectId;
        TestCasePanel.currentPanel._panel.webview.postMessage({
          type: 'loadProject',
          projectId,
        });
      }
      return;
    }

    // 创建新面板
    const panel = vscode.window.createWebviewPanel(
      TestCasePanel.viewType,
      'TestCase Manager',
      column || vscode.ViewColumn.One,
      {
        enableScripts: true,
        retainContextWhenHidden: true,
        localResourceRoots: [extensionUri],
      }
    );

    TestCasePanel.currentPanel = new TestCasePanel(panel, extensionUri, projectId);
  }

  private constructor(
    panel: vscode.WebviewPanel,
    extensionUri: vscode.Uri,
    projectId?: string
  ) {
    this._panel = panel;
    this._extensionUri = extensionUri;
    this._projectId = projectId;

    // 设置 webview 内容
    this._update();

    // 监听面板关闭
    this._panel.onDidDispose(() => this.dispose(), null, this._disposables);

    // 监听消息
    this._panel.webview.onDidReceiveMessage(
      async (message) => {
        switch (message.type) {
          case 'ready':
            if (this._projectId) {
              this._panel.webview.postMessage({
                type: 'loadProject',
                projectId: this._projectId,
              });
            }
            break;

          case 'showError':
            vscode.window.showErrorMessage(message.text);
            break;

          case 'showInfo':
            vscode.window.showInformationMessage(message.text);
            break;

          case 'commit':
            vscode.commands.executeCommand('testcase-manager.commit', message.projectId);
            break;

          case 'push':
            vscode.commands.executeCommand('testcase-manager.push', message.projectId);
            break;

          case 'pull':
            vscode.commands.executeCommand('testcase-manager.pull', message.projectId);
            break;
        }
      },
      null,
      this._disposables
    );
  }

  private _update() {
    const webview = this._panel.webview;
    this._panel.webview.html = getWebviewContent(webview, this._extensionUri);
  }

  public dispose() {
    TestCasePanel.currentPanel = undefined;

    this._panel.dispose();

    while (this._disposables.length) {
      const x = this._disposables.pop();
      if (x) {
        x.dispose();
      }
    }
  }
}
