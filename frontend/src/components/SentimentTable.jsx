import React, { useState, useEffect } from 'react'

export default function SentimentTable() {
  const [items, setItems] = useState([])
  const [amounts, setAmounts] = useState({})
  const [trades, setTrades] = useState(() => JSON.parse(localStorage.getItem('trades') || '{}'))

  useEffect(() => {
    const load = async () => {
      const res = await fetch('/api/feed')
      const data = await res.json()
      setItems(data)
      const conf = JSON.parse(localStorage.getItem('confidence_investment') || '{}')
      setAmounts(prev => {
        const next = { ...prev }
        data.forEach(it => {
          const c = Math.round(it.analysis?.confidence || 0)
          if (next[it.id] === undefined) next[it.id] = conf[c] || 0
        })
        return next
      })
    }
    load()
    const id = setInterval(load, 30000)
    return () => clearInterval(id)
  }, [])

  const updateAmount = (id, val) => {
    setAmounts({ ...amounts, [id]: val })
  }

  const execute = (it) => {
    const t = { action: it.analysis?.action || 'Buy', amount: Number(amounts[it.id] || 0), price: it.current }
    const next = { ...trades, [it.id]: t }
    setTrades(next)
    localStorage.setItem('trades', JSON.stringify(next))
  }

  const liveStatus = (it) => {
    const t = trades[it.id]
    if (!t) return '-'
    const diff = (it.current - t.price) / t.price * (t.action.toLowerCase() === 'sell' ? -1 : 1)
    return (diff * 100).toFixed(2) + '%'
  }

  const bookedPct = (it) => {
    const action = it.analysis?.action || 'Buy'
    const diff = (it.current - it.close) / it.close * (action.toLowerCase() === 'sell' ? -1 : 1)
    return (diff * 100).toFixed(2) + '%'
  }

  return (
    <div className="mt-4">
      <h3 className="font-semibold mb-2">Sentiment Signals</h3>
      <div className="overflow-x-auto">
        <table className="table-auto w-full text-sm">
          <thead>
            <tr>
              <th className="px-2">Feed</th>
              <th className="px-2">Rating</th>
              <th className="px-2">Close</th>
              <th className="px-2">Current</th>
              <th className="px-2">P&L</th>
              <th className="px-2">Amount</th>
              <th className="px-2">Trade</th>
              <th className="px-2">Live</th>
            </tr>
          </thead>
          <tbody>
            {items.map(it => (
              <tr key={it.id} className="odd:bg-gray-50 dark:odd:bg-gray-700">
                <td className="p-2">{it.title}</td>
                <td className="p-2 text-center">{it.analysis?.action}</td>
                <td className="p-2 text-right">{it.close?.toFixed(2)}</td>
                <td className="p-2 text-right">{it.current?.toFixed(2)}</td>
                <td className="p-2 text-right">{bookedPct(it)}</td>
                <td className="p-2">
                  <input type="number" className="w-full p-1 border rounded dark:bg-gray-800" value={amounts[it.id] || ''} onChange={e => updateAmount(it.id, e.target.value)} />
                </td>
                <td className="p-2 text-center">
                  <button onClick={() => execute(it)} className="px-2 py-1 bg-green-600 text-white rounded">
                    {it.analysis?.action}
                  </button>
                </td>
                <td className="p-2 text-right">{liveStatus(it)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

