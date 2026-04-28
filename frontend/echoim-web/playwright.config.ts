import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, devices } from '@playwright/test'

const frontendRoot = path.dirname(fileURLToPath(import.meta.url))
const workspaceRoot = path.resolve(frontendRoot, '..', '..')
const backendRoot = path.resolve(workspaceRoot, 'backend/echoim-server')
const frontendPort = Number(process.env.PLAYWRIGHT_FRONTEND_PORT ?? 4173)
const apiPort = Number(process.env.SERVER_PORT ?? 8080)
const wsPort = Number(process.env.ECHOIM_IM_PORT ?? 8091)

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 90_000,
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL: `http://127.0.0.1:${frontendPort}`,
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'mvn spring-boot:run',
      cwd: backendRoot,
      url: `http://127.0.0.1:${apiPort}/api/health`,
      reuseExistingServer: true,
      timeout: 240_000,
      env: {
        ...process.env,
        MYSQL_HOST: process.env.MYSQL_HOST ?? '127.0.0.1',
        MYSQL_PORT: process.env.MYSQL_PORT ?? '3306',
        MYSQL_DB: process.env.MYSQL_DB ?? 'echoim',
        MYSQL_USERNAME: process.env.MYSQL_USERNAME ?? 'root',
        MYSQL_PASSWORD: process.env.MYSQL_PASSWORD ?? 'root',
        REDIS_HOST: process.env.REDIS_HOST ?? '127.0.0.1',
        REDIS_PORT: process.env.REDIS_PORT ?? '6379',
        ECHOIM_JWT_SECRET:
          process.env.ECHOIM_JWT_SECRET ?? 'dev-echoim-secret-key-at-least-32-bytes-long',
        ECHOIM_FILE_STORAGE_TYPE: process.env.ECHOIM_FILE_STORAGE_TYPE ?? 'local',
        SERVER_PORT: String(apiPort),
        ECHOIM_IM_PORT: String(wsPort),
      },
    },
    {
      command: `npm run dev -- --host 127.0.0.1 --port ${frontendPort}`,
      cwd: frontendRoot,
      url: `http://127.0.0.1:${frontendPort}/login`,
      reuseExistingServer: true,
      timeout: 120_000,
      env: {
        ...process.env,
        VITE_ENABLE_E2E_HOOKS: 'true',
      },
    },
  ],
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
    },
  ],
})
