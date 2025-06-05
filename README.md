# Backtester

This project provides a simple Java Spring Boot backend and a basic HTML frontend for running trading strategy backtests using data from the Zerodha KITE API. Price data is cached in Redis to avoid redundant requests. A small in-memory history is kept for previous runs.

## Backend

The backend is located in the `backend` directory. It exposes a REST endpoint `/api/backtest` that accepts strategy name, symbol, period, date range and initial capital. Supported strategies include RSI, MACD, Bollinger Bands and Supertrend. Results are computed using simple implementations and include final capital and drawdown statistics.

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
