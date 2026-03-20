import type { ReactNode } from 'react'
import '../App.css'

interface LayoutProps {
  children: ReactNode
  sidebar: ReactNode
}

export function Layout({ children, sidebar }: LayoutProps) {
  return (
    <div className="layout">
      <aside className="layout-sidebar">{sidebar}</aside>
      <main className="layout-content">{children}</main>
    </div>
  )
}
