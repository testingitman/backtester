# Backtester

This project provides a simple Java Spring Boot backend and a basic HTML frontend for running trading strategy backtests using data from the Zerodha KITE API. Price data is cached in Redis to avoid redundant requests. A small in-memory history is kept for previous runs.

## Backend

The backend is located in the `backend` directory. It exposes a REST endpoint `/api/backtest` that accepts strategy name, symbol, candle period (e.g. `1d`, `1m`, `5m`, `30m`, `1w`), date range and initial capital. Supported strategies now cover a wide selection including:

* `RSI`, `MACD`, `BollingerBands`, `Supertrend`
* `RSI_SMA`, `MACD_CROSS`, `Bollinger_Reversal`, `Golden_Cross`
* `Breakout`, `ADX_DI`, `VWAP_Pullback`, `RSI_EMA`
* `MACD_Hist`, `Opening_Range`, `Scalp_Supertrend`, `MA_Ribbon`, `Mean_VWAP`

All algorithms operate on any candle period provided.

Price quotes are fetched from Zerodha KITE in `QuoteService` and cached in Redis and an in-memory map. Set `KITE_API_KEY` and `KITE_ACCESS_TOKEN` environment variables before running. History of executed backtests can be retrieved from `/api/backtest/history`.

## Frontend

Open `frontend/index.html` in a browser. It provides a small form to run backtests and view history.

## Building

This is a standard Maven project:

```bash
cd backend
mvn package
```

Running the Spring Boot application will serve the API on `localhost:8080`.
