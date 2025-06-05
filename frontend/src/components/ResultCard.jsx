import React from 'react'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

export default function ResultCard({ result }) {
  if (!result) return null
  return (
    <div className="mt-4 p-4 bg-white dark:bg-gray-800 rounded shadow">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center mb-4">
        <div>CAGR: {result.cagr?.toFixed(2)}%</div>
        <div>Drawdown: {result.drawdown?.toFixed(2)}%</div>
        <div>Sharpe Ratio: {result.sharpe?.toFixed(2)}</div>
      </div>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={result.equity.map((y, i) => ({ i, y }))}>
            <defs>
              <linearGradient id="colorEquity" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis dataKey="i" className="text-xs" />
            <YAxis className="text-xs" />
            <CartesianGrid strokeDasharray="3 3" />
            <Tooltip />
            <Area type="monotone" dataKey="y" stroke="#3b82f6" fillOpacity={1} fill="url(#colorEquity)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
