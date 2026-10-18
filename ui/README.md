# Calculator Collection UI

This is the standalone React/TypeScript frontend for Calculator Collection. It
uses the Calculator Collector API and contains no duplicate business or
persistence logic.

## Run locally

Start the Python API from the repository root:

```bash
python -m python_migration.api_server
```

In another terminal:

```bash
cd ui
npm install
npm run dev
```

Open the local Vite URL shown in the terminal. In development, Vite proxies
`/api` requests to `http://127.0.0.1:8001`.

## API configuration

The API base defaults to `/api`. Set `VITE_API_BASE_URL` to a full API base URL
(including `/api`) when the frontend and API are hosted separately. Configure
the API's `CALCOL_CORS_ORIGINS` environment variable with the frontend origin
for cross-origin deployments.

Create a production bundle with `npm run build`; the output is written to
`dist/`.
