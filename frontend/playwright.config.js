import { defineConfig } from '@playwright/test';

const chromeExecutablePath = process.env.PLAYWRIGHT_CHROME_EXECUTABLE_PATH;

export default defineConfig({
    testDir: './tests/e2e',
    fullyParallel: false,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
    use: {
        baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173',
        trace: 'on-first-retry',
        ...(chromeExecutablePath ? {
            launchOptions: {
                executablePath: chromeExecutablePath
            }
        } : {})
    }
});
