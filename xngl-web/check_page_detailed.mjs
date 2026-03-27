import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    await page.goto('http://localhost:5173/', { waitUntil: 'networkidle', timeout: 15000 });
    
    const title = await page.title();
    const url = page.url();
    
    console.log('✅ Page loaded successfully');
    console.log('Title:', title);
    console.log('URL:', url);
    
    // 检查是否有错误信息
    const errorElements = await page.locator('[role="alert"], .error, .ant-alert-error').count();
    console.log('Error elements found:', errorElements);
    
    // 检查是否有加载中的指示
    const spinners = await page.locator('.ant-spin, [role="progressbar"]').count();
    console.log('Loading spinners:', spinners);
    
    // 等待3秒让用户看到页面
    await page.waitForTimeout(3000);
    
    await page.screenshot({ path: './page_detailed.png' });
    console.log('Screenshot saved to ./page_detailed.png');
    
  } catch (e) {
    console.error('❌ Error:', e.message);
  } finally {
    await browser.close();
  }
})();
