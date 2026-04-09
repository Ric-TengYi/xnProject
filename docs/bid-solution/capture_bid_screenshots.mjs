import { chromium } from '../../xngl-web/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:5173';
const outputDir = path.resolve('docs/bid-solution/05-商务技术文件章节/assets/screenshots');

const pages = [
  { path: '/', file: 'dashboard-overview.png' },
  { path: '/contracts', file: 'contracts-management.png' },
  { path: '/projects', file: 'projects-management.png' },
  { path: '/projects/permits', file: 'permits-management.png' },
  { path: '/sites', file: 'sites-management.png' },
  { path: '/vehicles', file: 'vehicles-management.png' },
  { path: '/alerts', file: 'alerts-monitor.png' },
  { path: '/settings/platform-integrations', file: 'platform-integrations.png' },
  { path: '/mobile-workbench', file: 'mobile-workbench.png' },
];

const prototypeHtmlPath = path.resolve(
  'docs/bid-solution/05-商务技术文件章节/assets/mobile-prototypes/mini-program-showcase.html'
);
const prototypeOutputDir = path.resolve(
  'docs/bid-solution/05-商务技术文件章节/assets/mobile-prototypes'
);
const solutionGraphicsHtmlPath = path.resolve(
  'docs/bid-solution/05-商务技术文件章节/assets/solution-graphics/integration-security-showcase.html'
);
const solutionGraphicsOutputDir = path.resolve(
  'docs/bid-solution/05-商务技术文件章节/assets/solution-graphics'
);

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function login(page) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username: 'demo',
      password: '123456',
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

async function capture() {
  await ensureDir(outputDir);
  await ensureDir(prototypeOutputDir);
  await ensureDir(solutionGraphicsOutputDir);
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    deviceScaleFactor: 1.5,
  });
  const page = await context.newPage();
  await login(page);

  for (const target of pages) {
    console.log(`capturing ${target.file}`);
    await page.goto(`${baseUrl}${target.path}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.screenshot({
      path: path.join(outputDir, target.file),
      fullPage: true,
    });
    console.log(target.file);
  }

  const prototypePage = await context.newPage();
  await prototypePage.goto(`file://${prototypeHtmlPath}`, { waitUntil: 'load' });
  await prototypePage.setViewportSize({ width: 1680, height: 1280 });
  await prototypePage.waitForTimeout(800);

  const whole = prototypePage.locator('[data-shot="showcase"]');
  await whole.screenshot({
    path: path.join(prototypeOutputDir, 'mini-program-showcase.png'),
  });
  console.log('mini-program-showcase.png');

  const shots = [
    ['checkin', 'mini-program-checkin.png'],
    ['disposal', 'mini-program-disposal.png'],
    ['event', 'mini-program-event.png'],
    ['tracking', 'mini-program-tracking.png'],
  ];
  for (const [shot, file] of shots) {
    await prototypePage.locator(`[data-shot="${shot}"]`).screenshot({
      path: path.join(prototypeOutputDir, file),
    });
    console.log(file);
  }

  const graphicsPage = await context.newPage();
  await graphicsPage.goto(`file://${solutionGraphicsHtmlPath}`, { waitUntil: 'load' });
  await graphicsPage.setViewportSize({ width: 1680, height: 1360 });
  await graphicsPage.waitForTimeout(800);
  for (const [shot, file] of [
    ['integration', 'integration-architecture-showcase.png'],
    ['security', 'security-ops-showcase.png'],
  ]) {
    await graphicsPage.locator(`[data-shot="${shot}"]`).screenshot({
      path: path.join(solutionGraphicsOutputDir, file),
    });
    console.log(file);
  }

  await browser.close();
}

capture().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
