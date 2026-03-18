import { useRef, useEffect, useState } from 'react'
import { HotTable } from '@handsontable/react'
import { registerAllModules } from 'handsontable/registry'
import type { TestCase } from '../api/types'
import 'handsontable/dist/handsontable.full.min.css'

// 注册所有 Handsontable 模块
registerAllModules()

interface TestCaseTableProps {
  testCases: TestCase[]
  loading: boolean
  currentSuite: string | null
  onChange: (testCases: TestCase[]) => void
  onSave: () => void
  onSuiteChange: (suite: string) => void
}

const PRIORITY_OPTIONS = ['P0', 'P1', 'P2', 'P3']
const STATUS_OPTIONS = ['草稿', '评审中', '已发布', '已废弃']

const SUITES = ['默认套件', '登录模块', '支付模块', '订单模块', '用户中心']

export function TestCaseTable({
  testCases,
  loading,
  currentSuite,
  onChange,
  onSave,
  onSuiteChange,
}: TestCaseTableProps) {
  const hotRef = useRef<HotTable>(null)
  const [data, setData] = useState<any[]>([])

  // 将 TestCase 转换为表格数据
  useEffect(() => {
    const tableData = testCases.map((tc) => [
      tc.id,
      tc.title,
      tc.precondition,
      tc.steps,
      tc.expectedResult,
      tc.priority,
      tc.status,
      tc.tags?.join(', ') || '',
    ])
    setData(tableData)
  }, [testCases])

  const handleAfterChange = (changes: any[] | null) => {
    if (!changes) return

    const newTestCases = [...testCases]
    
    changes.forEach(([row, prop, , newValue]) => {
      const colIndex = typeof prop === 'number' ? prop : parseInt(prop)
      
      if (row >= newTestCases.length) {
        // 新增行
        const newTestCase: TestCase = {
          id: '',
          title: '',
          precondition: '',
          steps: '',
          expectedResult: '',
          priority: 'P2',
          status: '草稿',
          tags: [],
        }
        
        switch (colIndex) {
          case 0: newTestCase.id = newValue; break
          case 1: newTestCase.title = newValue; break
          case 2: newTestCase.precondition = newValue; break
          case 3: newTestCase.steps = newValue; break
          case 4: newTestCase.expectedResult = newValue; break
          case 5: newTestCase.priority = newValue; break
          case 6: newTestCase.status = newValue; break
          case 7: newTestCase.tags = newValue.split(',').map((t: string) => t.trim()).filter(Boolean); break
        }
        
        newTestCases.push(newTestCase)
      } else {
        // 更新现有行
        const testCase = { ...newTestCases[row] }
        
        switch (colIndex) {
          case 0: testCase.id = newValue; break
          case 1: testCase.title = newValue; break
          case 2: testCase.precondition = newValue; break
          case 3: testCase.steps = newValue; break
          case 4: testCase.expectedResult = newValue; break
          case 5: testCase.priority = newValue; break
          case 6: testCase.status = newValue; break
          case 7: testCase.tags = newValue.split(',').map((t: string) => t.trim()).filter(Boolean); break
        }
        
        newTestCases[row] = testCase
      }
    })

    onChange(newTestCases)
  }

  const handleAddRow = () => {
    const newTestCase: TestCase = {
      id: `TC${String(testCases.length + 1).padStart(3, '0')}`,
      title: '新建用例',
      precondition: '',
      steps: '',
      expectedResult: '',
      priority: 'P2',
      status: '草稿',
      tags: [],
    }
    onChange([...testCases, newTestCase])
  }

  const colHeaders = [
    '用例编号',
    '用例标题',
    '前置条件',
    '测试步骤',
    '预期结果',
    '优先级',
    '状态',
    '标签',
  ]

  const columns = [
    { data: 0, type: 'text', width: 100 },
    { data: 1, type: 'text', width: 200 },
    { data: 2, type: 'text', width: 150 },
    { data: 3, type: 'text', width: 200 },
    { data: 4, type: 'text', width: 200 },
    { 
      data: 5, 
      type: 'dropdown', 
      width: 80,
      source: PRIORITY_OPTIONS,
    },
    { 
      data: 6, 
      type: 'dropdown', 
      width: 100,
      source: STATUS_OPTIONS,
    },
    { data: 7, type: 'text', width: 120 },
  ]

  if (loading) {
    return <div className="loading">加载中...</div>
  }

  return (
    <div className="testcase-container">
      <div className="testcase-toolbar">
        <span className="testcase-toolbar-label">套件:</span>
        <select
          className="testcase-toolbar-select"
          value={currentSuite || '默认套件'}
          onChange={(e) => onSuiteChange(e.target.value)}
        >
          {SUITES.map((suite) => (
            <option key={suite} value={suite}>{suite}</option>
          ))}
        </select>
        <button className="testcase-toolbar-button primary" onClick={handleAddRow}>
          + 新建用例
        </button>
        <button className="testcase-toolbar-button" onClick={onSave}>
          保存
        </button>
      </div>

      <div className="testcase-table-wrapper">
        <HotTable
          ref={hotRef}
          data={data}
          colHeaders={colHeaders}
          columns={columns}
          rowHeaders={true}
          height="calc(100vh - 200px)"
          width="100%"
          stretchH="all"
          manualColumnResize={true}
          manualRowResize={true}
          contextMenu={{
            items: {
              row_above: {},
              row_below: {},
              remove_row: {},
              separator: { name: '---------' },
              copy: {},
              cut: {},
            },
          }}
          copyPaste={true}
          afterChange={handleAfterChange}
          licenseKey="non-commercial-and-evaluation"
        />
      </div>
    </div>
  )
}
