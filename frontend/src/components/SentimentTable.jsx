import React, { useState, useEffect } from 'react'

export default function SentimentTable() {
  const [items, setItems] = useState([])
  const [trades, setTrades] = useState(() => JSON.parse(localStorage.getItem('trades') || '{}'))
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const load = async () => {
      const res = await fetch('/api/feed')
      const data = await res.json()
      setItems(data)
      // previous version pre-filled trade amounts based on confidence
    }
    load()
    const id = setInterval(load, 10000)
    return () => clearInterval(id)
  }, [])

  const refresh = async () => {
    setLoading(true)
    await fetch('/api/feed/remote')
    await fetch('/api/feed').then(res => res.json()).then(setItems)
    setLoading(false)
  }

  const execute = (it) => {
    const t = { action: it.analysis?.action || 'Buy', amount: 1, price: it.current }
    const next = { ...trades, [it.id]: t }
    setTrades(next)
    localStorage.setItem('trades', JSON.stringify(next))
  }

  // previous table used liveStatus/bookedPct; kept for backward compatibility

  const relatedLinks = (it) => {
    if (!it.analysis?.tokens) return null
    const ids = items.filter(o => o.id !== it.id && o.analysis?.tokens?.some(t => it.analysis.tokens.includes(t)))
    if (!ids.length) return 'None'
    return ids.map(o => <a key={o.id} href={`#${o.id}`} className="underline mr-1">{o.title}</a>)
  }

  return (
    <div className="mt-4">
      <div className="flex items-center justify-between mb-2">
        <h3 className="font-semibold">Sentiment Signals</h3>
        <button onClick={refresh} disabled={loading} className="px-2 py-1 bg-blue-600 text-white rounded">
          {loading ? 'Updating...' : 'Update'}
        </button>
      </div>
      <div className="overflow-x-auto">
        <table className="table-auto w-full text-sm">
          <thead>
            <tr>
              <th className="px-2">News</th>
              <th className="px-2">Analysis</th>
              <th className="px-2">Last</th>
              <th className="px-2">Current</th>
              <th className="px-2">Action</th>
              <th className="px-2">Related</th>
              <th className="px-2">Trades</th>
            </tr>
          </thead>
          <tbody>
            {items.map(it => (
              <tr key={it.id} id={it.id} className="odd:bg-gray-50 dark:odd:bg-gray-700">
                <td className="p-2">
                  <a href={it.link} target="_blank" rel="noopener noreferrer" className="underline">
                    {it.title}
                  </a>
                  <div className="text-xs opacity-70">{new Date(it.timestamp * 1000).toLocaleString()}</div>
                </td>
                <td className="p-2 text-sm">{it.analysis?.reason}</td>
                <td className="p-2 text-right">{it.close?.toFixed(2)}</td>
                <td className="p-2 text-right">{it.current?.toFixed(2)}</td>
                <td className="p-2 text-center">
                  <button onClick={() => execute(it)} className="px-2 py-1 bg-green-600 text-white rounded">
                    {it.analysis?.action || 'Buy'}
                  </button>
                </td>
                <td className="p-2">{relatedLinks(it)}</td>
                <td className="p-2 text-sm">
                  {trades[it.id] ? `${trades[it.id].action}@${trades[it.id].price} x${trades[it.id].amount}` : 'No trades taken'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

