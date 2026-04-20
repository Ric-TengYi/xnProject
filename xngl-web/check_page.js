const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  try {
    await page.goto('http://localhost:5173/', { waitUntil: 'networkidle', timeout: 10000 });
    await page.screenshot({ path: './page.png' });
    
    const title = await page.title();
    console.log('✅ Page loaded successfully');
    console.log('Title:', title);
    console.log('Screenshot saved to ./page.png');
  } catch (e) {
    console.error('❌ Error:', e.message);
  } finally {
    await browser.close();
  }
})();
