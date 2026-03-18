import { useState, useRef } from 'react';
import './ImportExport.css';

interface ImportExportProps {
  projectId: string;
  suiteId?: string;
  onImportSuccess?: () => void;
}

export function ImportExport({ projectId, suiteId, onImportSuccess }: ImportExportProps) {
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [importResult, setImportResult] = useState<{
    success: boolean;
    imported: number;
    total: number;
    errors: any[];
  } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3001';

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setImporting(true);
    setImportResult(null);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (suiteId) {
        formData.append('suiteId', suiteId);
      }

      const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/import`, {
        method: 'POST',
        body: formData,
      });

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.error || '导入失败');
      }

      setImportResult({
        success: true,
        imported: result.imported,
        total: result.total,
        errors: result.errors || [],
      });
      
      if (result.errors?.length > 0) {
        console.warn('导入警告:', result.errors);
      }
      
      onImportSuccess?.();
    } catch (error: any) {
      setImportResult({
        success: false,
        imported: 0,
        total: 0,
        errors: [{ message: error.message }],
      });
    } finally {
      setImporting(false);
      // 重置文件输入
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleExport = async (format: 'xlsx' | 'csv' = 'xlsx') => {
    setExporting(true);
    try {
      let url = `${API_BASE_URL}/api/projects/${projectId}/export?format=${format}`;
      if (suiteId) {
        url = `${API_BASE_URL}/api/projects/${projectId}/export/${suiteId}?format=${format}`;
      }

      const response = await fetch(url);
      
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || '导出失败');
      }

      // 获取文件名
      const contentDisposition = response.headers.get('content-disposition');
      const filenameMatch = contentDisposition?.match(/filename="(.+)"/);
      const filename = filenameMatch?.[1] || `testcases.${format}`;

      // 下载文件
      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error: any) {
      alert(`导出失败: ${error.message}`);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="import-export-container">
      <input
        type="file"
        ref={fileInputRef}
        accept=".xlsx,.xls,.csv"
        onChange={handleImport}
        style={{ display: 'none' }}
      />
      
      <div className="import-export-toolbar">
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={importing}
          className="btn btn-import"
          title="支持 .xlsx, .xls, .csv 格式"
        >
          {importing ? (
            <>
              <span className="spinner"></span>
              导入中...
            </>
          ) : (
            <>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              导入 Excel
            </>
          )}
        </button>
        
        <div className="export-buttons">
          <button
            onClick={() => handleExport('xlsx')}
            disabled={exporting}
            className="btn btn-export"
          >
            {exporting ? (
              <>
                <span className="spinner"></span>
                导出中...
              </>
            ) : (
              <>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                导出 Excel
              </>
            )}
          </button>
          
          <button
            onClick={() => handleExport('csv')}
            disabled={exporting}
            className="btn btn-export-csv"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            CSV
          </button>
        </div>
      </div>
      
      {importResult && (
        <div className={`import-result ${importResult.success ? 'success' : 'error'}`}>
          {importResult.success ? (
            <>
              <div className="result-header">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                导入成功
              </div>
              <div className="result-details">
                成功导入 <strong>{importResult.imported}</strong> 条测试用例
                {importResult.total > 0 && (
                  <span>（共 {importResult.total} 行）</span>
                )}
              </div>
              {importResult.errors.length > 0 && (
                <div className="result-warnings">
                  警告：{importResult.errors.length} 个错误
                  <ul>
                    {importResult.errors.slice(0, 5).map((err, idx) => (
                      <li key={idx}>第 {err.row + 1} 行: {err.message}</li>
                    ))}
                    {importResult.errors.length > 5 && (
                      <li>...还有 {importResult.errors.length - 5} 个错误</li>
                    )}
                  </ul>
                </div>
              )}
            </>
          ) : (
            <>
              <div className="result-header">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="15" y1="9" x2="9" y2="15"/>
                  <line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
                导入失败
              </div>
              <div className="result-details">
                {importResult.errors[0]?.message || '未知错误'}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
