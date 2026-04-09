import { chromium } from '../../../xngl-web/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:5173';
const outputDir = path.resolve('docs/bid-solution/demo-video/frames');
const viewport = { width: 1600, height: 900 };

const routes = [
  { path: '/', file: '01-dashboard.png' },
  { path: '/projects', file: '02-projects.png' },
  { path: '/projects/1', file: '03-project-detail.png' },
  { path: '/settings/platform-integrations', file: '04-platform-integrations.png' },
  { path: '/projects/1?tab=permits', file: '05-project-permits.png' },
  { path: '/projects/permits?projectId=1', file: '06-permits-management.png' },
  { path: '/contracts/51', file: '07-contract-detail.png' },
  { path: '/contracts/settlements', file: '08-settlements.png' },
  { path: '/vehicles/tracking', file: '09-vehicle-tracking.png' },
  { path: '/alerts', file: '10-alerts.png' },
  { path: '/alerts/events', file: '11-events.png' },
  { path: '/mobile-showcase.html?screen=login', file: '12-mobile-login.png' },
  { path: '/mobile-showcase.html?screen=home', file: '13-mobile-home.png' },
  { path: '/mobile-showcase.html?screen=punch-in', file: '14-mobile-punch-in.png' },
  { path: '/mobile-showcase.html?screen=disposal', file: '15-mobile-disposal.png' },
  { path: '/mobile-showcase.html?screen=event-report', file: '16-mobile-event-report.png' },
  { path: '/mobile-showcase.html?screen=vehicle-tracking', file: '17-mobile-vehicle.png' },
];

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function login(page) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'admin',
      loginType: 'ACCOUNT',
      tenantId: '1',
    }),
  });
  const payload = await response.json();
  if (!payload?.data?.token || !payload?.data?.user) {
    throw new Error(`login failed: ${JSON.stringify(payload)}`);
  }
  await page.addInitScript((authPayload) => {
    localStorage.setItem('token', authPayload.token);
    localStorage.setItem('userInfo', JSON.stringify(authPayload.user));
  }, payload.data);
}

async function capturePage(page, route, file, waitMs = 3000) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(waitMs);
  await page.screenshot({
    path: path.join(outputDir, file),
    fullPage: false,
  });
  console.log(file);
}

async function main() {
  await ensureDir(outputDir);
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport });
  const page = await context.newPage();
  await login(page);

  for (const item of routes) {
    await capturePage(page, item.path, item.file);
  }

  await context.close();
  await browser.close();
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
