import { chromium } from '../../../xngl-web/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import path from 'node:path';

const baseUrl = 'http://127.0.0.1:5173';
const outputDir = path.resolve('docs/bid-solution/demo-video/output');
const rawVideoPath = path.join(outputDir, 'formal-demo-raw.webm');

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function smoothScroll(page, total, steps = 8) {
  for (let index = 0; index < steps; index += 1) {
    await page.mouse.wheel(0, total / steps);
    await page.waitForTimeout(500);
  }
}

async function hold(page, ms, options = {}) {
  const segments = Math.max(1, Math.ceil(ms / 5000));
  for (let index = 0; index < segments; index += 1) {
    if (options.scrollY) {
      await page.mouse.wheel(0, options.scrollY / segments);
    }
    await page.waitForTimeout(Math.ceil(ms / segments));
  }
}

async function gotoPage(page, route, holdMs, options = {}) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(options.stabilizeMs || 3500);
  if (options.scrollY) {
    await smoothScroll(page, options.scrollY, options.scrollSteps || 8);
  }
  await hold(page, holdMs, { scrollY: options.holdScrollY || 0 });
}

async function clickIfVisible(locator, timeout = 4000) {
  try {
    await locator.waitFor({ state: 'visible', timeout });
    await locator.click();
    return true;
  } catch {
    return false;
  }
}

async function runMobileSequence(page) {
  await page.goto(`${baseUrl}/mobile-workbench`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(4000);

  await clickIfVisible(page.getByRole('button', { name: '获取验证码' }));
  await page.waitForTimeout(2500);
  await clickIfVisible(page.getByRole('button', { name: '登录移动端' }));
  await page.waitForTimeout(7000);

  await hold(page, 25000, { scrollY: 600 });
  await clickIfVisible(page.getByText('现场', { exact: true }));
  await page.waitForTimeout(1500);

  await clickIfVisible(page.getByRole('button', { name: '提交出土拍照' }));
  await page.waitForTimeout(5000);
  await smoothScroll(page, 900, 6);
  await clickIfVisible(page.getByRole('button', { name: '确认消纳并留痕' }));
  await page.waitForTimeout(5000);
  await smoothScroll(page, 950, 6);
  await clickIfVisible(page.getByRole('button', { name: '提交事件上报' }));
  await page.waitForTimeout(5000);
  await clickIfVisible(page.getByRole('button', { name: '提交问题反馈' }));
  await page.waitForTimeout(5000);

  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
  await page.waitForTimeout(2500);
  await clickIfVisible(page.getByText('车辆', { exact: true }));
  await page.waitForTimeout(1500);
  await hold(page, 30000, { scrollY: 500 });

  await page.evaluate(() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }));
  await page.waitForTimeout(3000);
  await hold(page, 35000, { scrollY: 500 });
}

async function main() {
  await ensureDir(outputDir);
  const loginJsonPath =
    process.env.DEMO_LOGIN_JSON_PATH || path.join(outputDir, 'pc-login.json');
  const loginPayload = JSON.parse(await fs.readFile(loginJsonPath, 'utf8'));
  const demoToken = loginPayload?.data?.token;
  const demoUserInfo = loginPayload?.data?.user;
  if (!demoToken || !demoUserInfo) {
    throw new Error(`登录文件无效: ${loginJsonPath}`);
  }
  const auth = {
    token: demoToken,
    user: demoUserInfo,
  };

  const browser = await chromium.launch({
    headless: true,
    args: ['--window-size=1600,900'],
  });
  const context = await browser.newContext({
    viewport: { width: 1600, height: 900 },
    recordVideo: {
      dir: outputDir,
      size: { width: 1600, height: 900 },
    },
  });
  const page = await context.newPage();
  await page.addInitScript((authPayload) => {
    localStorage.setItem('token', authPayload.token);
    localStorage.setItem('userInfo', JSON.stringify(authPayload.user));
  }, auth);

  await gotoPage(page, '/', 45000, { scrollY: 900, scrollSteps: 6 });
  await gotoPage(page, '/projects', 30000, { scrollY: 800, scrollSteps: 6 });
  await gotoPage(page, '/projects/1', 40000, { scrollY: 1100, scrollSteps: 8 });
  await gotoPage(page, '/settings/platform-integrations', 25000, { scrollY: 1100, scrollSteps: 8 });

  const syncButton = page.getByRole('button', { name: '发起同步' });
  if (await clickIfVisible(syncButton, 5000)) {
    await page.waitForTimeout(1500);
    await clickIfVisible(page.getByRole('button', { name: 'OK' }), 4000);
    await page.waitForTimeout(7000);
  }
  await hold(page, 35000, { scrollY: 900 });

  await gotoPage(page, '/projects/1?tab=permits', 45000, { scrollY: 700, scrollSteps: 5 });
  await gotoPage(page, '/projects/permits?projectId=1', 50000, { scrollY: 1200, scrollSteps: 8 });
  await gotoPage(page, '/contracts/51', 45000, { scrollY: 1400, scrollSteps: 8 });
  await gotoPage(page, '/contracts/settlements', 35000, { scrollY: 900, scrollSteps: 6 });
  await gotoPage(page, '/vehicles/tracking', 45000, { scrollY: 1000, scrollSteps: 6 });
  await gotoPage(page, '/alerts', 35000, { scrollY: 900, scrollSteps: 6 });
  await gotoPage(page, '/alerts/events', 40000, { scrollY: 900, scrollSteps: 6 });
  await runMobileSequence(page);
  await gotoPage(page, '/alerts/events', 35000, { scrollY: 800, scrollSteps: 5 });
  await gotoPage(page, '/', 25000, { scrollY: 600, scrollSteps: 4 });

  const video = page.video();
  await context.close();
  await browser.close();
  const videoPath = await video.path();
  await fs.copyFile(videoPath, rawVideoPath);
  console.log(rawVideoPath);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
