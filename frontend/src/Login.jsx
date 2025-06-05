import React from 'react'

export default function Login() {
  const start = () => {
    window.location.href = '/api/auth/login'
  }
  return (
    <div className="h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900">
      <button onClick={start} className="px-4 py-2 bg-blue-600 text-white rounded">Login with Zerodha</button>
    </div>
  )
}
