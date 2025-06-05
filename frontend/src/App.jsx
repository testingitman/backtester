import React, { useState, useEffect } from 'react'
import Navbar from './components/Navbar.jsx'
import Sidebar from './components/Sidebar.jsx'
import Dashboard from './components/Dashboard.jsx'

export default function App() {
  const [dark, setDark] = useState(false)
  const [sidebar, setSidebar] = useState(false)
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])
  return (
    <div className="flex h-screen">
      <Sidebar open={sidebar} onClose={() => setSidebar(false)} />
      <div className="flex flex-col flex-1">
        <Navbar dark={dark} onToggle={() => setDark(!dark)} onMenu={() => setSidebar(true)} />
        <main className="flex-1 p-4 overflow-y-auto bg-gray-100 dark:bg-gray-900">
          <Dashboard />
        </main>
      </div>
    </div>
  )
}
