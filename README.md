# Backtester

This project provides a Java Spring Boot backend and a React frontend for running trading strategy backtests using data from the Zerodha KITE API. Price data is cached in Redis to avoid redundant requests. A small in-memory history is kept for previous runs.

## Backend

The backend is located in the `backend` directory. It exposes a REST endpoint `/api/backtest` that accepts strategy name, symbol, candle period (e.g. `1d`, `1m`, `5m`, `30m`, `1w`), date range and initial capital. Supported strategies now cover a wide selection including:

* `RSI`, `MACD`, `BollingerBands`, `Supertrend`
* `RSI_SMA`, `MACD_CROSS`, `Bollinger_Reversal`, `Golden_Cross`
* `Breakout`, `ADX_DI`, `VWAP_Pullback`, `RSI_EMA`
* `MACD_Hist`, `Opening_Range`, `Scalp_Supertrend`, `MA_Ribbon`, `Mean_VWAP`

All algorithms operate on any candle period provided.

Price quotes are fetched from Zerodha KITE in `QuoteService` and cached in Redis and an in-memory map. Credentials are read from `config.yaml`. History of executed backtests can be retrieved from `/api/backtest/history`.

## Frontend

The frontend lives under `frontend` and is built with [Vite](https://vitejs.dev/).
During development run:

```bash
cd frontend
npm install
npm run dev
```

For deployment (e.g. on a DigitalOcean VM) build the static files and serve the
`dist` directory with any web server:

```bash
npm run build
npx serve -s dist
```

## Building

This is a standard Maven project:

```bash
cd backend
mvn package
```

Running the Spring Boot application will serve the API on `localhost:8080`.

## RSS Monitor

The `rss_monitor.py` script polls the feed defined in `config.yaml` every five minutes,
analyzes new headlines with GPT-4 and stores the result in Redis. Summaries are
also posted to the configured Telegram channel.

Install Python dependencies and run the monitor with:

```bash
pip install -r requirements.txt
python rss_monitor.py
```
