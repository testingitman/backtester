import React, { useEffect, useState } from 'react'

export default function OrderDialog({ open, onClose, item, onPlaced }) {
  const [funds, setFunds] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [price, setPrice] = useState(item?.current || 0)

  useEffect(() => {
    if (!open) return
    fetch('/api/order/funds')
      .then(res => res.json())
      .then(d => setFunds(d.funds || 0))
  }, [open])

  useEffect(() => {
    if (item) {
      setPrice(item.current || 0)
      setQuantity(1)
    }
  }, [item])

  const onQtyChange = e => {
    const q = parseInt(e.target.value || '0', 10)
    setQuantity(q)
    if (q > 0) setPrice((funds / q).toFixed(2))
  }

  const onPriceChange = e => {
    const p = parseFloat(e.target.value || '0')
    setPrice(p)
    if (p > 0) setQuantity(Math.floor(funds / p))
  }

  const place = async () => {
    await fetch('/api/order', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        symbol: item.analysis?.tokens?.[0] || item.symbol,
        action: item.analysis?.action || 'BUY',
        quantity: quantity,
        price: price,
      })
    })
    if (onPlaced) onPlaced({ action: item.analysis?.action || 'Buy', amount: quantity, price })
    onClose()
  }

  if (!open) return null
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-30">
      <div className="bg-white dark:bg-gray-800 p-4 rounded shadow w-72 space-y-4">
        <h3 className="font-semibold text-lg">Place Order</h3>
        <div>Available: {funds.toFixed(2)}</div>
        <div>
          <label className="block text-sm">Quantity</label>
          <input type="number" value={quantity} onChange={onQtyChange} className="mt-1 w-full p-2 rounded border dark:bg-gray-700" />
        </div>
        <div>
          <label className="block text-sm">Price</label>
          <input type="number" value={price} onChange={onPriceChange} className="mt-1 w-full p-2 rounded border dark:bg-gray-700" />
        </div>
        <div className="flex justify-end space-x-2">
          <button onClick={onClose} className="px-3 py-1 bg-gray-300 rounded">Cancel</button>
          <button onClick={place} className="px-3 py-1 bg-blue-600 text-white rounded">Place Order</button>
        </div>
      </div>
    </div>
  )
}
