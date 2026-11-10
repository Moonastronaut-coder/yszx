import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: '../tests/e2e',
  use: { browserName: 'chromium', trace: 'retain-on-failure' },
  webServer: [
    {
      command: 'node ../scripts/dev.mjs backend',
      url: 'http://127.0.0.1:8080/actuator/health/readiness',
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: 'node ../scripts/dev.mjs portal',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: false,
    },
    {
      command: 'node ../scripts/dev.mjs admin',
      url: 'http://127.0.0.1:5174',
      reuseExistingServer: false,
    },
  ],
});
