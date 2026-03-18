import { createContext, useContext, useReducer, type ReactNode } from 'react'
import type { Project } from '../api/types'

interface AppState {
  currentProject: Project | null
  currentBranch: string
  currentSuite: string
  isDirty: boolean
  loading: boolean
  error: string | null
}

type AppAction =
  | { type: 'SET_PROJECT'; payload: Project | null }
  | { type: 'SET_BRANCH'; payload: string }
  | { type: 'SET_SUITE'; payload: string }
  | { type: 'SET_DIRTY'; payload: boolean }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }

const initialState: AppState = {
  currentProject: null,
  currentBranch: 'main',
  currentSuite: '默认套件',
  isDirty: false,
  loading: false,
  error: null,
}

function appReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'SET_PROJECT':
      return { ...state, currentProject: action.payload, isDirty: false }
    case 'SET_BRANCH':
      return { ...state, currentBranch: action.payload }
    case 'SET_SUITE':
      return { ...state, currentSuite: action.payload }
    case 'SET_DIRTY':
      return { ...state, isDirty: action.payload }
    case 'SET_LOADING':
      return { ...state, loading: action.payload }
    case 'SET_ERROR':
      return { ...state, error: action.payload }
    default:
      return state
  }
}

interface AppContextValue {
  state: AppState
  dispatch: React.Dispatch<AppAction>
}

const AppContext = createContext<AppContextValue | undefined>(undefined)

interface AppProviderProps {
  children: ReactNode
}

export function AppProvider({ children }: AppProviderProps) {
  const [state, dispatch] = useReducer(appReducer, initialState)

  return (
    <AppContext.Provider value={{ state, dispatch }}>
      {children}
    </AppContext.Provider>
  )
}

export function useAppState() {
  const context = useContext(AppContext)
  if (context === undefined) {
    throw new Error('useAppState must be used within an AppProvider')
  }
  return context
}
