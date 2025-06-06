# Backtester

This project provides a Java Spring Boot backend and a React frontend for running trading strategy backtests using data from the Zerodha KITE API. Price data is cached in Redis to avoid redundant requests. A small in-memory history is kept for previous runs.

## Prerequisites

 - Java 8 or later with Maven
- Node.js 18+ and npm
- Python 3.11+ (for `rss_monitor.py`)
- A Redis server running on `localhost:6379`
- API keys configured in `config.yaml` for Zerodha KITE (key, secret and redirect URI), OpenAI and Telegram

## Backend

The backend is located in the `backend` directory. It exposes a REST endpoint `/api/backtest` that accepts strategy name, symbol, candle period (e.g. `1d`, `1m`, `5m`, `30m`, `1w`), date range and initial capital. Supported strategies now cover a wide selection including:

* `RSI`, `MACD`, `BollingerBands`, `Supertrend`
* `RSI_SMA`, `MACD_CROSS`, `Bollinger_Reversal`, `Golden_Cross`
* `Breakout`, `ADX_DI`, `VWAP_Pullback`, `RSI_EMA`
* `MACD_Hist`, `Opening_Range`, `Scalp_Supertrend`, `MA_Ribbon`, `Mean_VWAP`

All algorithms operate on any candle period provided.

Price quotes are fetched from Zerodha KITE in `QuoteService` and cached in Redis and an in-memory map. Credentials are read from `config.yaml`. History of executed backtests can be retrieved from `/api/backtest/history`.

Before starting the backend, update `config.yaml` with your personal API keys and ensure Redis is running. During development the application can be started with:

```bash
cd backend
mvn spring-boot:run
```

## Frontend

The frontend lives under `frontend` and is built with [Vite](https://vitejs.dev/).
During development run:

```bash
cd frontend
npm install
npm run dev
# The dev server proxies all `/api` requests to `http://localhost:8080`,
# so make sure the Spring Boot backend is running.
```

For deployment (e.g. on a DigitalOcean VM) build the static files and serve the
`dist` directory with any web server:

```bash
npm run build
npx serve -s dist
```

## Building

This is a standard Maven project targeting Java 8:

```bash
cd backend
mvn package
```

The generated JAR can be started with:

```bash
java -jar target/*.jar
```

The API will then be available on `localhost:8080`.

## Zerodha Login

Set `kite_api_key`, `kite_api_secret` and `kite_redirect_uri` in `config.yaml`.
If you want to access the portal from other machines set `host: '0.0.0.0'` in
`frontend/vite.config.js` and update `kite_redirect_uri` to match your public
IP or domain (e.g. `https://172.232.119.157:8080/api/auth/callback`). Restart the
Spring Boot backend and the Vite dev server after making these changes. Then
open `http://172.232.119.157:5173/login` and click **Login with Zerodha** to begin the
OAuth flow. On success the backend stores the returned access token in memory
for subsequent API calls.

All requests to the Kite API must include the access token in an `Authorization`
header of the form `token <api_key>:<access_token>`. Do not pass the token in
the query string.

Set `frontend_url` in `config.yaml` to the address where the React app is served
(default `http://172.232.119.157:5173/`). The backend redirects here after successful
authentication.

## RSS Monitor

The `rss_monitor.py` script polls the feed defined in `config.yaml` every five minutes,
analyzes new headlines with the Groq API and stores the result in Redis. Each analysis
includes affected NSE tokens, a buy or sell recommendation with a confidence score
and whether the effect is short or long term. Summaries are also posted to the configured Telegram channel.

Edit `config.yaml` with your Groq API key and Telegram credentials before running and make sure Redis is running.

Install Python dependencies and run the monitor with:

```bash
pip install -r requirements.txt
python rss_monitor.py
```
