import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import Login from './Login.jsx'
import './index.css'

const root = document.getElementById('root')
const path = window.location.pathname
ReactDOM.createRoot(root).render(
  <React.StrictMode>
    {path.startsWith('/login') ? <Login /> : <App />}
  </React.StrictMode>
)
