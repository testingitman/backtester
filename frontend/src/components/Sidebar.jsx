import React from 'react'

export default function Sidebar({ open, onClose, onNav }) {
  return (
    <div className={`fixed inset-y-0 left-0 z-20 transform bg-gray-200 dark:bg-gray-800 w-48 p-4 space-y-4 md:static md:translate-x-0 transition-transform ${open ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}> 
      <button className="md:hidden mb-4" onClick={onClose} aria-label="Close menu">✕</button>
      <nav className="space-y-2">
        <a href="#" onClick={e => { e.preventDefault(); onNav('backtest'); }} className="flex items-center space-x-2 hover:text-blue-600"><span>📈</span><span>Backtest</span></a>
        <a href="#" onClick={e => { e.preventDefault(); onNav('sentiment'); }} className="flex items-center space-x-2 hover:text-blue-600"><span>💬</span><span>Sentiment</span></a>
        <a href="#" className="flex items-center space-x-2 hover:text-blue-600"><span>📜</span><span>History</span></a>
      </nav>
    </div>
  )
}
