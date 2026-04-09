import asyncio
from playwright.async_api import async_playwright
import os

async def run_tests():
    os.makedirs('test-screenshots/cmp-83', exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(viewport={'width': 1280, 'height': 800})
        page = await context.new_page()
        
        print("Logging in...")
        await page.goto('http://localhost:5173/login')
        await page.wait_for_load_state('networkidle')
        await page.fill('input[placeholder="请输入账号"]', 'admin')
        await page.fill('input[placeholder="请输入密码"]', 'admin')
        await page.click('button[type="submit"], button:has-text("登录")')
        await page.wait_for_timeout(3000)
        print(f"Logged in successfully. URL: {page.url}")
        
        # 1. 处置证同步
        print("Testing 处置证同步...")
        await page.goto('http://localhost:5173/project/permit')
        await page.wait_for_timeout(2000)
        await page.screenshot(path='test-screenshots/cmp-83/1_permit_sync.png')
        
        # 2. GPS 定位对接
        print("Testing GPS 定位对接...")
        await page.goto('http://localhost:5173/vehicle/monitor')
        await page.wait_for_timeout(2000)
        await page.screenshot(path='test-screenshots/cmp-83/2_gps_sync.png')
        
        # 3. 地磅对接
        print("Testing 地磅对接...")
        await page.goto('http://localhost:5173/device/weighbridge')
        await page.wait_for_timeout(2000)
        await page.screenshot(path='test-screenshots/cmp-83/3_weighbridge_sync.png')
        
        # 4. 视频对接
        print("Testing 视频对接...")
        await page.goto('http://localhost:5173/device/video')
        await page.wait_for_timeout(2000)
        await page.screenshot(path='test-screenshots/cmp-83/4_video_sync.png')
        
        # 5. SSO/统一认证
        print("Testing SSO/统一认证...")
        await page.goto('http://localhost:5173/system/sso')
        await page.wait_for_timeout(2000)
        await page.screenshot(path='test-screenshots/cmp-83/5_sso_sync.png')
        
        await browser.close()
        print("Tests completed.")

if __name__ == '__main__':
    asyncio.run(run_tests())
