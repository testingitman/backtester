import React, { useState, useEffect } from 'react'

export default function SentimentTable() {
  const [items, setItems] = useState([])
  const [trades, setTrades] = useState(() => JSON.parse(localStorage.getItem('trades') || '{}'))
  const [loading, setLoading] = useState(false)
  const [dialog, setDialog] = useState(null)
  const [query, setQuery] = useState('')
  const [answer, setAnswer] = useState('')

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

  const clearFeeds = async () => {
    await fetch('/api/feed', { method: 'DELETE' })
    setItems([])
  }

  const runQuery = async () => {
    if (!query.trim()) return
    const res = await fetch('/api/grok', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ q: query })
    })
    const data = await res.json()
    setAnswer(data.answer || JSON.stringify(data))
  }

  const openDialog = async (symbol, key, action, price) => {
    setDialog({ symbol, key, action, price, amount: price, quantity: 1, balance: 0 })
    try {
      const res = await fetch('/api/order/balance')
      const data = await res.json()
      if (data.cash) {
        setDialog(d => ({ ...d, balance: data.cash }))
      }
    } catch (err) {
      console.error('Failed to fetch balance', err)
    }
  }

  const updatePrice = v => {
    setDialog(d => ({ ...d, price: v, amount: v * d.quantity }))
  }

  const updateQty = v => {
    setDialog(d => ({ ...d, quantity: v, amount: v * d.price }))
  }

  const placeOrder = async () => {
    if (!dialog) return
    const body = { symbol: dialog.symbol, side: dialog.action, quantity: Math.round(dialog.quantity), price: dialog.price }
    await fetch('/api/order/place', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
    const t = { action: dialog.action, amount: dialog.amount, price: dialog.price }
    const next = { ...trades, [dialog.key]: t }
    setTrades(next)
    localStorage.setItem('trades', JSON.stringify(next))
    setDialog(null)
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
      <div className="flex flex-wrap items-center justify-between mb-2 space-y-2 md:space-y-0">
        <h3 className="font-semibold">Sentiment Signals</h3>
        <div className="flex items-center space-x-2">
          <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Ask Grok" className="p-1 border rounded dark:bg-gray-700" />
          <button onClick={runQuery} className="px-2 py-1 bg-purple-600 text-white rounded">Ask</button>
          <button onClick={clearFeeds} className="px-2 py-1 bg-red-600 text-white rounded">Clear</button>
          <button onClick={refresh} disabled={loading} className="px-2 py-1 bg-blue-600 text-white rounded">
            {loading ? 'Updating...' : 'Update'}
          </button>
        </div>
      </div>
      {answer && <div className="mb-2 text-sm p-2 bg-gray-200 dark:bg-gray-700 rounded">{answer}</div>}
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
                        <button onClick={() => openDialog(it.stocks[0].symbol, `${it.id}-${it.stocks[0].symbol}`, it.stocks[0].action || 'Buy', it.stocks[0].current)} className="px-2 py-1 bg-green-600 text-white rounded">
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
                      <button onClick={() => openDialog(s.symbol, `${it.id}-${s.symbol}`, s.action || 'Buy', s.current)} className="px-2 py-1 bg-green-600 text-white rounded">
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
      {dialog && (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white dark:bg-gray-800 p-4 rounded space-y-2 w-72">
            <div className="text-sm">Balance: {dialog.balance}</div>
            <div>
              <label className="block text-sm">Price</label>
              <input type="number" className="w-full p-1 border rounded dark:bg-gray-700" value={dialog.price}
                onChange={e => updatePrice(Number(e.target.value))} />
            </div>
            <div>
              <label className="block text-sm">Quantity</label>
              <input type="number" className="w-full p-1 border rounded dark:bg-gray-700" value={dialog.quantity}
                onChange={e => updateQty(Number(e.target.value))} />
            </div>
            <div className="text-sm">Amount: {dialog.amount.toFixed(2)}</div>
            <div className="flex justify-end space-x-2 pt-2">
              <button onClick={placeOrder} className="px-3 py-1 bg-blue-600 text-white rounded">Place Order</button>
              <button onClick={() => setDialog(null)} className="px-3 py-1 bg-gray-300 dark:bg-gray-700 rounded">Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

