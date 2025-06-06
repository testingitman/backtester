import React, { useState, useEffect } from 'react'
import ResultCard from './ResultCard.jsx'
import SentimentTable from './SentimentTable.jsx'

function ConfidenceTable() {
  const stored = JSON.parse(localStorage.getItem('confidence_investment') || '{}')
  const [amounts, setAmounts] = useState(
    Array.from({ length: 10 }, (_, i) => stored[i + 1] || 0)
  )
  const update = (i, v) => {
    const next = [...amounts]
    next[i] = v
    setAmounts(next)
  }
  const save = () => {
    const obj = {}
    amounts.forEach((v, i) => (obj[i + 1] = Number(v)))
    localStorage.setItem('confidence_investment', JSON.stringify(obj))
    alert('Saved')
  }
  return (
    <div className="mt-4">
      <h3 className="font-semibold mb-2">Investment per Confidence</h3>
      <table className="table-auto w-full text-sm">
        <thead>
          <tr><th className="px-2">Score</th><th className="px-2">Amount</th></tr>
        </thead>
        <tbody>
          {amounts.map((val, i) => (
            <tr key={i} className="odd:bg-gray-50 dark:odd:bg-gray-700">
              <td className="p-2 text-center">{i + 1}</td>
              <td className="p-2">
                <input type="number" className="w-full p-1 border rounded dark:bg-gray-800" value={val} onChange={e => update(i, e.target.value)} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button onClick={save} className="mt-2 px-3 py-1 bg-blue-600 text-white rounded">Save</button>
    </div>
  )
}

export default function Dashboard() {
  const [form, setForm] = useState({ strategy: 'RSI', symbol: '', period: 'day', start: '', end: '', capital: 100000 })
  const [errors, setErrors] = useState({})
  const [result, setResult] = useState(null)
  const [instruments, setInstruments] = useState([])

  const strategies = ['RSI', 'MACD', 'BollingerBands', 'Supertrend']
  const periods = ['minute', 'day', '3minute', '5minute', '10minute', '15minute', '30minute', '60minute']

  useEffect(() => {
    const load = async () => {
      const res = await fetch('/api/instruments')
      const data = await res.json()
      data.sort((a, b) => a.name.localeCompare(b.name))
      setInstruments(data)
      if (data.length && !form.symbol) {
        setForm(f => ({ ...f, symbol: data[0].token }))
      }
    }
    load()
  }, [])

  const updateForm = e => setForm({ ...form, [e.target.name]: e.target.value })

  const validate = () => {
    const e = {}
    if (!form.symbol) e.symbol = 'Symbol required'
    if (!form.start) e.start = 'Start required'
    if (!form.end) e.end = 'End required'
    return e
  }

  const submit = async e => {
    e.preventDefault()
    const e2 = validate()
    setErrors(e2)
    if (Object.keys(e2).length) return
    const params = new URLSearchParams(form).toString()
    const res = await fetch('/api/backtest?' + params)
    const data = await res.json()
    setResult({ cagr: data.cagr ?? 0, drawdown: data.drawdown ?? 0, sharpe: data.sharpe ?? 0, equity: data.prices ?? [] })
  }

  return (
    <div>
      <form onSubmit={submit} className="bg-white dark:bg-gray-800 p-4 rounded shadow space-y-4">
        <div>
          <label className="block text-sm">Strategy</label>
          <select name="strategy" value={form.strategy} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700">
            {strategies.map(s => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-sm">Symbol</label>
          <select name="symbol" value={form.symbol} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700">
            {instruments.map(i => (
              <option key={i.token} value={i.token}>{i.name}</option>
            ))}
          </select>
          {errors.symbol && <div className="text-red-500 text-xs">{errors.symbol}</div>}
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm">Period</label>
            <select name="period" value={form.period} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700">
              {periods.map(p => <option key={p}>{p}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm">Capital</label>
            <input type="number" name="capital" value={form.capital} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700" />
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm">From</label>
            <input type="date" name="start" value={form.start} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700" />
            {errors.start && <div className="text-red-500 text-xs">{errors.start}</div>}
          </div>
          <div>
            <label className="block text-sm">To</label>
            <input type="date" name="end" value={form.end} onChange={updateForm} className="mt-1 w-full p-2 rounded border dark:bg-gray-700" />
            {errors.end && <div className="text-red-500 text-xs">{errors.end}</div>}
          </div>
        </div>
        <div className="text-right">
          <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition">Run</button>
        </div>
      </form>
      <ResultCard result={result} />
      <ConfidenceTable />
      <SentimentTable />
    </div>
  )
}

