import React from 'react'

export default function Navbar({ dark, onToggle, onMenu }) {
  return (
    <header className="flex items-center justify-between px-4 h-12 bg-white dark:bg-gray-800 shadow">
      <button className="md:hidden" onClick={onMenu} aria-label="Menu">
        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
      </button>
      <h1 className="font-semibold text-lg">Backtester</h1>
      <button onClick={onToggle} aria-label="Toggle Theme" className="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-700">
        {dark ? '🌙' : '☀️'}
      </button>
    </header>
  )
}
