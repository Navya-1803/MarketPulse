# MarketPulse Frontend

React + TypeScript UI for the MarketPulse smart watchlist.

## Stack

- Vite, React, TypeScript
- React Router
- Axios
- Bootstrap (base) plus a custom dark theme

## Run

Start the backend first, then:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to `http://localhost:8080`.

## Auth

JWT is stored in `localStorage` and attached as `Authorization: Bearer <token>`. Protected routes redirect to `/login`.

## Tests

```powershell
npm test
```
