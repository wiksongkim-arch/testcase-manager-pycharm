import * as vscode from 'vscode';

export function getWebviewContent(
  webview: vscode.Webview,
  extensionUri: vscode.Uri
): string {
  const config = vscode.workspace.getConfiguration('testcaseManager');
  const apiUrl = config.get<string>('apiUrl') || 'http://localhost:3001';

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>TestCase Manager</title>
  
  <!-- Handsontable CSS -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/handsontable@14.0.0/dist/handsontable.full.min.css">
  
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
      background: var(--vscode-editor-background);
      color: var(--vscode-editor-foreground);
      padding: 16px;
    }
    
    .container {
      max-width: 100%;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid var(--vscode-panel-border);
      margin-bottom: 16px;
    }
    
    .title {
      font-size: 18px;
      font-weight: 600;
    }
    
    .toolbar {
      display: flex;
      gap: 8px;
      align-items: center;
      padding: 12px;
      background: var(--vscode-editor-inactiveSelectionBackground);
      border-radius: 6px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    
    .toolbar-group {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    
    .toolbar-label {
      font-size: 13px;
      color: var(--vscode-descriptionForeground);
    }
    
    select {
      padding: 6px 12px;
      border: 1px solid var(--vscode-input-border);
      background: var(--vscode-input-background);
      color: var(--vscode-input-foreground);
      border-radius: 4px;
      font-size: 13px;
      cursor: pointer;
    }
    
    select:focus {
      outline: none;
      border-color: var(--vscode-focusBorder);
    }
    
    button {
      padding: 6px 16px;
      border: 1px solid var(--vscode-button-border);
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
      border-radius: 4px;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
    }
    
    button:hover {
      background: var(--vscode-button-hoverBackground);
    }
    
    button.primary {
      background: var(--vscode-button-background);
      font-weight: 500;
    }
    
    button.secondary {
      background: transparent;
      border-color: var(--vscode-button-secondaryBackground);
      color: var(--vscode-button-secondaryForeground);
    }
    
    button.secondary:hover {
      background: var(--vscode-button-secondaryBackground);
    }
    
    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    
    .table-container {
      flex: 1;
      overflow: auto;
      border: 1px solid var(--vscode-panel-border);
      border-radius: 6px;
    }
    
    .handsontable {
      font-size: 13px;
    }
    
    .handsontable .htCore td {
      color: var(--vscode-editor-foreground);
      background: var(--vscode-editor-background);
    }
    
    .handsontable .htCore th {
      background: var(--vscode-editor-inactiveSelectionBackground);
      color: var(--vscode-editor-foreground);
      font-weight: 600;
    }
    
    .handsontable .htCore .htDimmed {
      color: var(--vscode-descriptionForeground);
    }
    
    .handsontable .htCore .currentRow {
      background: var(--vscode-list-activeSelectionBackground);
    }
    
    .handsontable .htCore .currentCol {
      background: var(--vscode-list-activeSelectionBackground);
    }
    
    .handsontable .htCore .htSelected {
      background: var(--vscode-editor-selectionBackground) !important;
    }
    
    .status-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: var(--vscode-statusBar-background);
      color: var(--vscode-statusBar-foreground);
      border-radius: 4px;
      margin-top: 12px;
      font-size: 12px;
    }
    
    .status-item {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .status-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    
    .status-indicator.connected {
      background: #28a745;
    }
    
    .status-indicator.disconnected {
      background: #dc3545;
    }
    
    .status-indicator.syncing {
      background: #ffc107;
      animation: pulse 1s infinite;
    }
    
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
    
    .loading {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 200px;
      font-size: 14px;
      color: var(--vscode-descriptionForeground);
    }
    
    .empty-state {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      height: 100%;
      color: var(--vscode-descriptionForeground);
    }
    
    .empty-state h3 {
      margin-bottom: 12px;
      font-weight: 500;
    }
    
    .conflict-badge {
      background: var(--vscode-errorForeground);
      color: white;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 11px;
      font-weight: 600;
    }
    
    .divider {
      width: 1px;
      height: 24px;
      background: var(--vscode-panel-border);
      margin: 0 8px;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <span class="title">📋 TestCase Manager</span>
      <span id="projectName"></span>
    </div>
    
    <div class="toolbar">
      <div class="toolbar-group">
        <span class="toolbar-label">套件:</span>
        <select id="suiteSelect">
          <option value="">加载中...</option>
        </select>
      </div>
      
      <div class="divider"></div>
      
      <div class="toolbar-group">
        <button id="addRowBtn" class="primary">+ 新建用例</button>
        <button id="saveBtn">💾 保存</button>
      </div>
      
      <div class="divider"></div>
      
      <div class="toolbar-group">
        <button id="commitBtn" class="secondary">📦 提交</button>
        <button id="pullBtn" class="secondary">⬇️ 拉取</button>
        <button id="pushBtn" class="secondary">⬆️ 推送</button>
      </div>
      
      <div class="toolbar-group" style="margin-left: auto;">
        <span id="conflictBadge" class="conflict-badge" style="display: none;">0 冲突</span>
      </div>
    </div>
    
    <div class="table-container">
      <div id="testcaseTable"></div>
    </div>
    
    <div class="status-bar">
      <div class="status-item">
        <span id="connectionStatus" class="status-indicator disconnected"></span>
        <span id="statusText">未连接</span>
      </div>
      <div class="status-item">
        <span id="rowCount">0 行</span>
      </div>
      <div class="status-item">
        <span id="lastSaved">未保存</span>
      </div>
    </div>
  </div>
  
  <!-- Handsontable JS -->
  <script src="https://cdn.jsdelivr.net/npm/handsontable@14.0.0/dist/handsontable.full.min.js"></script>
  
  <script>
    const vscode = acquireVsCodeApi();
    const API_URL = '${apiUrl}';
    
    let hot = null;
    let currentProjectId = null;
    let currentSuite = '默认套件';
    let testCases = [];
    let suites = ['默认套件'];
    let hasUnsavedChanges = false;
    
    // 优先级和状态选项
    const PRIORITY_OPTIONS = ['P0', 'P1', 'P2', 'P3'];
    const STATUS_OPTIONS = ['草稿', '评审中', '已发布', '已废弃'];
    
    // 列定义
    const colHeaders = ['用例编号', '用例标题', '前置条件', '测试步骤', '预期结果', '优先级', '状态', '标签'];
    const columns = [
      { data: 0, type: 'text', width: 100 },
      { data: 1, type: 'text', width: 200 },
      { data: 2, type: 'text', width: 150 },
      { data: 3, type: 'text', width: 200 },
      { data: 4, type: 'text', width: 200 },
      { data: 5, type: 'dropdown', width: 80, source: PRIORITY_OPTIONS },
      { data: 6, type: 'dropdown', width: 100, source: STATUS_OPTIONS },
      { data: 7, type: 'text', width: 120 }
    ];
    
    // 初始化 Handsontable
    function initTable() {
      const container = document.getElementById('testcaseTable');
      
      hot = new Handsontable(container, {
        data: [],
        colHeaders: colHeaders,
        columns: columns,
        rowHeaders: true,
        height: '100%',
        width: '100%',
        stretchH: 'all',
        manualColumnResize: true,
        manualRowResize: true,
        contextMenu: {
          items: {
            row_above: {},
            row_below: {},
            remove_row: {},
            separator: { name: '---------' },
            copy: {},
            cut: {},
          }
        },
        copyPaste: true,
        afterChange: handleAfterChange,
        licenseKey: 'non-commercial-and-evaluation'
      });
    }
    
    // 处理表格数据变化
    function handleAfterChange(changes, source) {
      if (source === 'loadData' || !changes) return;
      
      hasUnsavedChanges = true;
      updateStatus();
      
      // 同步更新 testCases 数组
      changes.forEach(([row, prop, oldVal, newVal]) => {
        if (!testCases[row]) {
          testCases[row] = {
            id: '',
            title: '',
            precondition: '',
            steps: '',
            expectedResult: '',
            priority: 'P2',
            status: '草稿',
            tags: []
          };
        }
        
        const colIndex = typeof prop === 'number' ? prop : parseInt(prop);
        switch (colIndex) {
          case 0: testCases[row].id = newVal; break;
          case 1: testCases[row].title = newVal; break;
          case 2: testCases[row].precondition = newVal; break;
          case 3: testCases[row].steps = newVal; break;
          case 4: testCases[row].expectedResult = newVal; break;
          case 5: testCases[row].priority = newVal; break;
          case 6: testCases[row].status = newVal; break;
          case 7: testCases[row].tags = newVal ? newVal.split(',').map(t => t.trim()).filter(Boolean) : []; break;
        }
      });
    }
    
    // 加载项目数据
    async function loadProject(projectId) {
      currentProjectId = projectId;
      updateConnectionStatus('syncing');
      
      try {
        // 加载项目信息
        const projectRes = await fetch(\`\${API_URL}/api/projects/\${projectId}\`);
        if (!projectRes.ok) throw new Error('加载项目失败');
        const project = await projectRes.json();
        document.getElementById('projectName').textContent = project.name;
        
        // 加载测试用例
        await loadTestCases();
        
        // 加载 Git 状态
        await loadGitStatus();
        
        updateConnectionStatus('connected');
      } catch (error) {
        console.error('加载项目失败:', error);
        updateConnectionStatus('disconnected');
        showError('加载项目失败: ' + error.message);
      }
    }
    
    // 加载测试用例
    async function loadTestCases() {
      if (!currentProjectId) return;
      
      try {
        const res = await fetch(\`\${API_URL}/api/projects/\${currentProjectId}/testcases?suite=\${encodeURIComponent(currentSuite)}\`);
        if (!res.ok) throw new Error('加载测试用例失败');
        testCases = await res.json();
        
        // 转换为表格数据
        const tableData = testCases.map(tc => [
          tc.id,
          tc.title,
          tc.precondition,
          tc.steps,
          tc.expectedResult,
          tc.priority,
          tc.status,
          tc.tags?.join(', ') || ''
        ]);
        
        hot.loadData(tableData);
        document.getElementById('rowCount').textContent = \`\${tableData.length} 行\`;
        hasUnsavedChanges = false;
        updateStatus();
      } catch (error) {
        console.error('加载测试用例失败:', error);
        showError('加载测试用例失败');
      }
    }
    
    // 加载 Git 状态
    async function loadGitStatus() {
      if (!currentProjectId) return;
      
      try {
        const res = await fetch(\`\${API_URL}/api/projects/\${currentProjectId}/git/status\`);
        if (!res.ok) return;
        const status = await res.json();
        
        // 更新冲突徽章
        const conflictBadge = document.getElementById('conflictBadge');
        if (status.conflictedCount > 0) {
          conflictBadge.textContent = \`\${status.conflictedCount} 冲突\`;
          conflictBadge.style.display = 'inline-block';
        } else {
          conflictBadge.style.display = 'none';
        }
      } catch (error) {
        console.error('加载 Git 状态失败:', error);
      }
    }
    
    // 保存测试用例
    async function saveTestCases() {
      if (!currentProjectId) return;
      
      updateConnectionStatus('syncing');
      
      try {
        const res = await fetch(\`\${API_URL}/api/projects/\${currentProjectId}/testcases\`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ testCases })
        });
        
        if (!res.ok) throw new Error('保存失败');
        
        hasUnsavedChanges = false;
        document.getElementById('lastSaved').textContent = '已保存 ' + new Date().toLocaleTimeString();
        updateStatus();
        showInfo('保存成功');
        updateConnectionStatus('connected');
      } catch (error) {
        console.error('保存失败:', error);
        updateConnectionStatus('disconnected');
        showError('保存失败: ' + error.message);
      }
    }
    
    // 添加新行
    function addNewRow() {
      const newId = \`TC\${String(testCases.length + 1).padStart(3, '0')}\`;
      const newTestCase = {
        id: newId,
        title: '新建用例',
        precondition: '',
        steps: '',
        expectedResult: '',
        priority: 'P2',
        status: '草稿',
        tags: []
      };
      
      testCases.push(newTestCase);
      
      const currentData = hot.getData();
      currentData.push([
        newId,
        '新建用例',
        '',
        '',
        '',
        'P2',
        '草稿',
        ''
      ]);
      
      hot.loadData(currentData);
      hasUnsavedChanges = true;
      updateStatus();
      
      // 滚动到新行
      hot.selectCell(currentData.length - 1, 1);
    }
    
    // 更新连接状态
    function updateConnectionStatus(status) {
      const indicator = document.getElementById('connectionStatus');
      const text = document.getElementById('statusText');
      
      indicator.className = 'status-indicator ' + status;
      
      switch (status) {
        case 'connected':
          text.textContent = '已连接';
          break;
        case 'disconnected':
          text.textContent = '未连接';
          break;
        case 'syncing':
          text.textContent = '同步中...';
          break;
      }
    }
    
    // 更新状态栏
    function updateStatus() {
      const saveBtn = document.getElementById('saveBtn');
      if (hasUnsavedChanges) {
        saveBtn.textContent = '💾 保存*';
        saveBtn.style.fontWeight = 'bold';
      } else {
        saveBtn.textContent = '💾 保存';
        saveBtn.style.fontWeight = 'normal';
      }
    }
    
    // 显示错误
    function showError(message) {
      vscode.postMessage({ type: 'showError', text: message });
    }
    
    // 显示信息
    function showInfo(message) {
      vscode.postMessage({ type: 'showInfo', text: message });
    }
    
    // 事件监听
    document.getElementById('addRowBtn').addEventListener('click', addNewRow);
    document.getElementById('saveBtn').addEventListener('click', saveTestCases);
    document.getElementById('commitBtn').addEventListener('click', () => {
      vscode.postMessage({ type: 'commit', projectId: currentProjectId });
    });
    document.getElementById('pullBtn').addEventListener('click', () => {
      vscode.postMessage({ type: 'pull', projectId: currentProjectId });
    });
    document.getElementById('pushBtn').addEventListener('click', () => {
      vscode.postMessage({ type: 'push', projectId: currentProjectId });
    });
    document.getElementById('suiteSelect').addEventListener('change', (e) => {
      currentSuite = e.target.value;
      loadTestCases();
    });
    
    // 监听来自 VS Code 的消息
    window.addEventListener('message', async (event) => {
      const message = event.data;
      
      switch (message.type) {
        case 'loadProject':
          await loadProject(message.projectId);
          break;
      }
    });
    
    // 初始化
    initTable();
    
    // 加载套件列表
    async function loadSuites() {
      // 这里可以从 API 加载套件列表
      // 暂时使用默认值
      const suiteSelect = document.getElementById('suiteSelect');
      suiteSelect.innerHTML = suites.map(s => \`<option value="\${s}">\${s}</option>\`).join('');
    }
    
    loadSuites();
    
    // 通知 VS Code webview 已就绪
    vscode.postMessage({ type: 'ready' });
  </script>
</body>
</html>`;
}
