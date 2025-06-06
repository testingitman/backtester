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

  const execute = (key, action, price) => {
    const t = { action, amount: 1, price }
    const next = { ...trades, [key]: t }
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
              <th className="px-2">Stock</th>
              <th className="px-2">Publish</th>
              <th className="px-2">Last</th>
              <th className="px-2">Conf.</th>
              <th className="px-2">Action</th>
              <th className="px-2">Trades</th>
            </tr>
          </thead>
          <tbody>
            {items.map(it => (
              <React.Fragment key={it.id}>
                <tr id={it.id} className="odd:bg-gray-50 dark:odd:bg-gray-700">
                  <td rowSpan={Math.max(it.stocks?.length || 1, 1)} className="p-2">
                    <a href={it.link} target="_blank" rel="noopener noreferrer" className="underline">
                      {it.title}
                    </a>
                    <div className="text-xs opacity-70">{new Date(it.timestamp * 1000).toLocaleString()}</div>
                  </td>
                  <td rowSpan={Math.max(it.stocks?.length || 1, 1)} className="p-2 text-sm">{it.analysis?.reason}</td>
                  {it.stocks && it.stocks.length ? (
                    <>
                      <td className="p-2">{it.stocks[0].symbol} ({it.stocks[0].token})</td>
                      <td className="p-2 text-right">{it.stocks[0].close?.toFixed(2)}</td>
                      <td className="p-2 text-right">{it.stocks[0].current?.toFixed(2)}</td>
                      <td className="p-2 text-center">{it.stocks[0].confidence?.toFixed(1)}</td>
                      <td className="p-2 text-center">
                        <button onClick={() => execute(`${it.id}-${it.stocks[0].symbol}`, it.stocks[0].action || 'Buy', it.stocks[0].current)} className="px-2 py-1 bg-green-600 text-white rounded">
                          {it.stocks[0].action || 'Buy'}
                        </button>
                      </td>
                      <td rowSpan={Math.max(it.stocks?.length || 1, 1)} className="p-2 text-sm">
                        {Object.entries(trades).filter(([k]) => k.startsWith(it.id + '-')).map(([k,v]) => `${k.split('-')[1]}:${v.action}@${v.price}`).join('; ') || 'No trades taken'}
                      </td>
                    </>
                  ) : (
                    <>
                      <td className="p-2" colSpan={5}>No stocks</td>
                      <td rowSpan={1} className="p-2 text-sm">No trades taken</td>
                    </>
                  )}
                </tr>
                {it.stocks && it.stocks.slice(1).map(s => (
                  <tr key={`${it.id}-${s.symbol}`} className="odd:bg-gray-50 dark:odd:bg-gray-700">
                    <td className="p-2">{s.symbol} ({s.token})</td>
                    <td className="p-2 text-right">{s.close?.toFixed(2)}</td>
                    <td className="p-2 text-right">{s.current?.toFixed(2)}</td>
                    <td className="p-2 text-center">{s.confidence?.toFixed(1)}</td>
                    <td className="p-2 text-center">
                      <button onClick={() => execute(`${it.id}-${s.symbol}`, s.action || 'Buy', s.current)} className="px-2 py-1 bg-green-600 text-white rounded">
                        {s.action || 'Buy'}
                      </button>
                    </td>
                  </tr>
                ))}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

